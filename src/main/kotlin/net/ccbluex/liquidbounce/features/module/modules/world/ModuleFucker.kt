/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.item.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.block.BlockState
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.RaycastContext
import kotlin.math.max

/**
 * Fucker module
 *
 * Destroys/Uses selected blocks around you.
 */
object ModuleFucker : Module("Fucker", Category.WORLD) {

    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).onChange {
        if (it > range) {
            range
        } else {
            it
        }
    }

    /**
     * Entrance requires the target block to have an entrance. It does not matter if we can see it or not.
     * If this condition is true, it will override the wall range to range
     * and act as if we were breaking normally.
     *
     * Useful for Hypixel and CubeCraft
     */
    private object FuckerEntrance : ToggleableConfigurable(this, "Entrance", false) {
        /**
         * Breaks the weakest block around target block and makes an entrance
         */
        val breakFree by boolean("BreakFree", true)
    }

    init {
        tree(FuckerEntrance)
    }

    private val surroundings by boolean("Surroundings", true)
    private val targets by blocks("Targets", findBlocksEndingWith("_BED", "DRAGON_EGG").toHashSet())
    private val delay by int("Delay", 0, 0..20, "ticks")
    private val action by enumChoice("Action", DestroyAction.DESTROY)
    private val forceImmediateBreak by boolean("ForceImmediateBreak", false)

    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    // Rotation
    private val rotations = tree(RotationsConfigurable())

    private object FuckerHighlight : ToggleableConfigurable(this, "Highlight", true) {

        private val color by color("Color", Color4b(255, 0, 0, 50))
        private val outlineColor by color("OutlineColor", Color4b(255, 0, 0, 100))

        private val fullBox = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack
            val (pos, _) = currentTarget ?: return@handler

            renderEnvironmentForWorld(matrixStack) {
                val blockState = pos.getState() ?: return@renderEnvironmentForWorld
                if (blockState.isAir) {
                    return@renderEnvironmentForWorld
                }

                val outlineShape = blockState.getOutlineShape(world, pos)
                val boundingBox = if (outlineShape.isEmpty) {
                    fullBox
                } else {
                    outlineShape.boundingBox
                }

                withPositionRelativeToCamera(pos.toVec3d()) {
                    withColor(color) {
                        drawSolidBox(boundingBox)
                    }

                    if (outlineColor.a != 0) {
                        withColor(outlineColor) {
                            drawOutlinedBox(boundingBox)
                        }
                    }
                }
            }
        }

    }

    init {
        tree(FuckerHighlight)
    }

    private var currentTarget: DestroyerTarget? = null

    val moduleRepeatable = repeatable {
        if (!ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            return@repeatable
        }

        val wasTarget = currentTarget

        updateTarget()

        if (wasTarget != null && currentTarget == null) {
            interaction.cancelBlockBreaking()
        }

        // Delay if the target changed - this also includes when introducing a new target from null.
        if (wasTarget != currentTarget) {
            waitTicks(delay)
        }

        // Check if blink is enabled - if so, we don't want to do anything.
        if (ModuleBlink.enabled) {
            return@repeatable
        }

        val destroyerTarget = currentTarget ?: return@repeatable
        val currentRotation = RotationManager.serverRotation

        // Check if we are already looking at the block
        val rayTraceResult = raytraceBlock(
            max(range, wallRange).toDouble(),
            currentRotation,
            destroyerTarget.pos,
            destroyerTarget.pos.getState() ?: return@repeatable
        ) ?: return@repeatable

        val raytracePos = rayTraceResult.blockPos

        // Check if the raytrace result includes a block, if not we don't want to deal with it.
        if (rayTraceResult.type != HitResult.Type.BLOCK ||
            raytracePos.getState()?.isAir == true || raytracePos != destroyerTarget.pos) {
            return@repeatable
        }

        // Use action should be used if the block is the same as the current target and the action is set to use.
        if (destroyerTarget.action == DestroyAction.USE) {
            if (interaction.interactBlock(player, Hand.MAIN_HAND, rayTraceResult) == ActionResult.SUCCESS) {
                player.swingHand(Hand.MAIN_HAND)
            }

            waitTicks(delay)
        } else {
            doBreak(rayTraceResult, immediate = forceImmediateBreak)
        }
    }

    private fun updateTarget() {
        val eyesPos = player.eyes

        val possibleBlocks = searchBlocksInCuboid(range + 1, eyesPos) { pos, state ->
            targets.contains(state.block) &&
                getNearestPoint(eyesPos, Box.enclosing(pos, pos.add(1, 1, 1))).distanceTo(eyesPos) <= range
        }

        val currentTarget = this.currentTarget
        if (currentTarget != null && possibleBlocks.any { (pos, _) -> pos == currentTarget.pos }) {
            // Stick with the current target because it's still valid.
            val currentTargetPos = currentTarget.pos
            val currentTargetState = currentTargetPos.getState()

            if (currentTargetState?.isAir == false && considerAsTarget(currentTargetPos, currentTargetState,
                    range.toDouble(), wallRange.toDouble(), action)) {
                chat("Falling back to current target")
                return
            }
        }
        this.currentTarget = null

        // Find the nearest block
        val (pos, state) = possibleBlocks.minByOrNull { (pos, _) -> pos.getCenterDistanceSquared() } ?: return

        val range = range.toDouble()
        var wallRange = wallRange.toDouble()

        // If the block has an entrance, we should ignore the wall range and act as if we are breaking normally.
        if (FuckerEntrance.enabled && pos.hasEntrance) {
            wallRange = range
        }

        if (!considerAsTarget(pos, state, range, wallRange, action)) {
            // Is there any block in the way?
            if (FuckerEntrance.enabled && FuckerEntrance.breakFree) {
                val weakBlock = pos.weakestBlock
                val weakState = weakBlock?.getState() ?: return

                considerAsTarget(weakBlock, weakState, range, range, DestroyAction.DESTROY)
            } else if (surroundings) {
                updateSurroundings(pos, state)
            }
        } else {
            chat("New target found without tricks")
        }
    }

    private fun considerAsTarget(
        blockPos: BlockPos,
        blockState: BlockState,
        range: Double,
        throughWallsRange: Double,
        action: DestroyAction
    ): Boolean {
        val raytrace = raytraceBlock(
            player.eyes,
            blockPos,
            blockState,
            range = range,
            wallsRange = throughWallsRange
        ) ?: return false

        val (rotation, _) = raytrace
        RotationManager.aimAt(
            rotation,
            considerInventory = !ignoreOpenInventory,
            configurable = rotations,
            Priority.IMPORTANT_FOR_USAGE_1,
            this@ModuleFucker
        )

        this.currentTarget = DestroyerTarget(blockPos, action)
        return true
    }

    private fun updateSurroundings(initialPosition: BlockPos, state: BlockState) {
        val raytraceResult = world.raycast(
            RaycastContext(
                player.eyes,
                initialPosition.toCenterPos(),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
            )
        ) ?: return

        if (raytraceResult.type != HitResult.Type.BLOCK) {
            return
        }

        val blockPos = raytraceResult.blockPos
        val blockState = blockPos.getState() ?: return

        if (blockState.isAir) {
            return
        }

        considerAsTarget(blockPos, blockState, range.toDouble(), wallRange.toDouble(), DestroyAction.DESTROY)
    }

    data class DestroyerTarget(val pos: BlockPos, val action: DestroyAction)

    enum class DestroyAction(override val choiceName: String) : NamedChoice {
        DESTROY("Destroy"), USE("Use")
    }

}
