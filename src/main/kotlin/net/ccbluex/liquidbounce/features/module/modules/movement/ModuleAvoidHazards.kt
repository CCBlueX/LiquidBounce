/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.movement.avoidhazards.AvoidHazardInputPlanner
import net.ccbluex.liquidbounce.features.module.modules.movement.avoidhazards.isLadderClimbState
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.isOnMagmaBlock
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.SAFETY_FEATURE
import net.ccbluex.liquidbounce.utils.math.iterateBlockPos
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.BasePressurePlateBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.CactusBlock
import net.minecraft.world.level.block.FireBlock
import net.minecraft.world.level.block.MagmaBlock
import net.minecraft.world.level.block.PowderSnowBlock
import net.minecraft.world.level.block.SweetBerryBushBlock
import net.minecraft.world.level.block.WebBlock
import net.minecraft.world.level.block.WitherRoseBlock
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.Shapes

/**
 * Anti hazards module
 *
 * Prevents you walking into blocks that might be malicious for you.
 */
object ModuleAvoidHazards : ClientModule("AvoidHazards", ModuleCategories.MOVEMENT) {
    private var mode by enumChoice("Mode", AvoidMode.SHAPE)
    private val avoid by multiEnumChoice("Avoid", Avoid.entries)

    // Conflicts with AvoidHazards
    val cobWebs get() = Avoid.COBWEB in avoid

    private const val MOVEMENT_PREDICTION_TICKS = 2
    private const val CACTUS_BLOCK_MARGIN = 0.001

    @Suppress("MagicNumber")
    private val UNSAFE_BLOCK_CAP = Block.box(
        0.0,
        0.0,
        0.0,
        16.0,
        4.0,
        16.0
    )

    @Suppress("unused")
    val shapeHandler = handler<BlockShapeEvent> { event ->
        if (mode != AvoidMode.SHAPE) {
            return@handler
        }

        avoid.find { it.test(event.state.block, event.state.fluidState, event.pos) }?.let {
            event.shape = if (it.fullCube) Shapes.block() else UNSAFE_BLOCK_CAP
        }
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent>(priority = SAFETY_FEATURE) { event ->
        if (mode != AvoidMode.INPUT || !event.directionalInput.isMoving) {
            return@handler
        }

        val activeAvoidModes = avoid
        if (activeAvoidModes.isEmpty()) {
            return@handler
        }

        event.directionalInput = AvoidHazardInputPlanner.chooseSafeInput(event.directionalInput) { candidate ->
            isSafeInput(
                directionalInput = candidate,
                jump = event.jump,
                sneak = event.sneak,
                avoidModes = activeAvoidModes
            )
        }
    }

    private fun isSafeInput(
        directionalInput: DirectionalInput,
        jump: Boolean,
        sneak: Boolean,
        avoidModes: Collection<Avoid>
    ): Boolean {
        val level = mc.level ?: return true

        val simulatedInput = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(
            directionalInput = directionalInput,
            jump = jump,
            sprinting = player.isSprinting,
            sneaking = sneak
        )

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(simulatedInput)
        simulatedPlayer.pos = player.position()
        var previousBoundingBox = simulatedPlayer.boundingBox
        // Do not reject every candidate while already on a ladder. We only block
        // transitions that newly enter climb-state.
        val startedOnLadder = Avoid.LADDERS in avoidModes && wouldEnterLadderClimbState(simulatedPlayer)

        repeat(MOVEMENT_PREDICTION_TICKS) {
            simulatedPlayer.tick()
            val currentBoundingBox = simulatedPlayer.boundingBox
            val sweptBoundingBox = previousBoundingBox.minmax(currentBoundingBox)
            val enteredLadder =
                Avoid.LADDERS in avoidModes &&
                    !startedOnLadder &&
                    wouldEnterLadderClimbState(simulatedPlayer)

            if (enteredLadder ||
                isHazardCollision(currentBoundingBox, level, avoidModes) ||
                isHazardCollision(sweptBoundingBox, level, avoidModes)
            ) {
                return false
            }

            previousBoundingBox = currentBoundingBox
        }

        return true
    }

    /**
     * Predict whether the simulated player would be in a vanilla climb-state
     * after this movement step.
     *
     * @see net.minecraft.world.entity.LivingEntity.onClimbable
     * @see isLadderClimbState
     */
    private fun wouldEnterLadderClimbState(simulatedPlayer: SimulatedPlayer): Boolean {
        return isLadderClimbStateAt(simulatedPlayer.pos.toBlockPos())
    }

    private fun isLadderClimbStateAt(pos: BlockPos): Boolean {
        val currentState = pos.getState() ?: return false
        return isLadderClimbState(currentState, pos.below().getState())
    }

    @Suppress("CognitiveComplexMethod")
    private fun isHazardCollision(
        boundingBox: AABB,
        level: ClientLevel,
        avoidModes: Collection<Avoid>
    ): Boolean {
        if (Avoid.MAGMA in avoidModes && boundingBox.isOnMagmaBlock()) {
            return true
        }

        return boundingBox.iterateBlockPos().any { pos ->
            val blockState = pos.getState() ?: return@any false
            val fluidState = blockState.fluidState
            val block = blockState.block

            avoidModes.any { avoidMode ->
                when (avoidMode) {
                    Avoid.MAGMA -> false
                    Avoid.LAVA -> {
                        if (!avoidMode.test(block, fluidState, pos)) {
                            false
                        } else {
                            val fluidShape = fluidState.getShape(level, pos)
                            !fluidShape.isEmpty && boundingBox.intersects(fluidShape.bounds().move(pos))
                        }
                    }
                    Avoid.CACTI -> {
                        if (!avoidMode.test(block, fluidState, pos)) {
                            false
                        } else {
                            // Cactus damage is handled by entity-inside logic, which can trigger on block-cell
                            // contact. Use the whole block cell for conservative prediction.
                            val expandedBox = boundingBox.inflate(CACTUS_BLOCK_MARGIN, 0.0, CACTUS_BLOCK_MARGIN)
                            expandedBox.intersects(pos)
                        }
                    }
                    else -> {
                        if (!avoidMode.test(block, fluidState, pos)) {
                            false
                        } else {
                            val shape = blockState.getShape(level, pos)
                            !shape.isEmpty && boundingBox.intersects(shape.bounds().move(pos))
                        }
                    }
                }
            }
        }
    }

    private enum class AvoidMode(
        override val tag: String,
        override val tagAliases: List<String> = emptyList(),
    ) : Tagged {
        SHAPE("Shape"),
        INPUT("Input", listOf("Movement")),
    }

    private enum class Avoid(
        override val tag: String,
        val fullCube: Boolean = true,
        val test: (block: Block, fluidState: FluidState, pos: BlockPos) -> Boolean
    ) : Tagged {
        CACTI("Cacti", test = { block, _, _ ->
            block is CactusBlock
        }),
        BERRY_BUSH("BerryBush", test = { block, _, _ ->
            block is SweetBerryBushBlock
        }),
        FIRE("Fire", test = { block, _, _ ->
            block is FireBlock
        }),
        COBWEB("Cobwebs", test = { block, _, _ ->
            block is WebBlock
        }),
        LADDERS("Ladders", test = { _, _, pos ->
            isLadderClimbStateAt(pos)
        }),
        PRESSURE_PLATES("PressurePlates", fullCube = false, test = { block, _, _ ->
            block is BasePressurePlateBlock
        }),
        MAGMA("MagmaBlocks", fullCube = false, test = { _, _, pos ->
            pos.below().getBlock() is MagmaBlock
        }),
        LAVA("Lava", test = { _, fluidState, _ ->
            fluidState.`is`(Fluids.LAVA) || fluidState.`is`(Fluids.FLOWING_LAVA)
        }),
        WITHER_ROSE("WitherRose", test = { block, _, _ ->
            block is WitherRoseBlock
        }),
        POWDER_SNOW("PowderSnow", test = { block, _, _ ->
            block is PowderSnowBlock
        })
    }
}
