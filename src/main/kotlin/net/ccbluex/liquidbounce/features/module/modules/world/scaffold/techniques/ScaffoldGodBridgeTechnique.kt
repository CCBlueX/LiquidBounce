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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getTargetedPosition
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.LedgeState
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldLedgeExtension
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique.NORMAL_INVESTIGATION_OFFSETS
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetFinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetFinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.EntityPose
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin

object ScaffoldGodBridgeTechnique : ScaffoldTechnique("GodBridge"), ScaffoldLedgeExtension {

    private enum class Mode(override val choiceName: String) : NamedChoice {
        JUMP("Jump"),
        SNEAK("Sneak")
    }

    private var mode by enumChoice("Mode", Mode.JUMP)
    private var sneakTime by int("SneakTime", 1, 1..10)

    override fun ledge(
        ledge: Boolean,
        ledgeSoon: Boolean,
        target: BlockPlacementTarget?,
        rotation: Rotation
    ): LedgeState {
        if (!isActive) {
            return LedgeState.NO_LEDGE
        }

        // todo: introduce rotation prediction because currently I abuse [howLongItTakes] to get the ticks
        //   and simply check for the correct rotation without considering the Rotation Manager at all
        val currentCrosshairTarget = raycast(3.0, rotation)

        if (target == null || currentCrosshairTarget == null) {
            if (ledgeSoon) {
                return LedgeState(requiresJump = false, requiresSneak = sneakTime)
            }
        } else if (ledge) {
            // Does the crosshair target meet the requirements?
            if (!target.doesCrosshairTargetFullFillRequirements(currentCrosshairTarget)
                || !ModuleScaffold.isValidCrosshairTarget(currentCrosshairTarget)) {
                return when (mode) {
                    Mode.JUMP -> LedgeState(requiresJump = true, requiresSneak = 0)
                    Mode.SNEAK -> LedgeState(requiresJump = false, requiresSneak = sneakTime)
                }
            }
        }

        return LedgeState.NO_LEDGE
    }

    private var isOnRightSide = false

    override fun findPlacementTarget(
        predictedPos: Vec3d,
        predictedPose: EntityPose,
        optimalLine: Line?,
        bestStack: ItemStack
    ): BlockPlacementTarget? {
        val searchOptions = BlockPlacementTargetFindingOptions(
            NORMAL_INVESTIGATION_OFFSETS,
            bestStack,
            CenterTargetPositionFactory,
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            predictedPos,
            predictedPose
        )

        return findBestBlockPlacementTarget(getTargetedPosition(predictedPos.toBlockPos()), searchOptions)
    }

    override fun getRotations(target: BlockPlacementTarget?): Rotation? {
        val dirInput = DirectionalInput(player.input)

        if (dirInput == DirectionalInput.NONE) {
            target ?: return null

            return getRotationForNoInput(target)
        }

        val direction = getMovementDirectionOfInput(player.yaw, dirInput) + 180

        // Round to 45°-steps (NORTH, NORTH_EAST, etc.)
        val movingYaw = round(direction / 45) * 45
        val isMovingStraight = movingYaw % 90 == 0f

        return if (isMovingStraight) {
            getRotationForStraightInput(movingYaw)
        } else {
            getRotationForDiagonalInput(movingYaw)
        }

    }

    private fun getRotationForStraightInput(movingYaw: Float): Rotation {
        if (player.isOnGround) {
            isOnRightSide = floor(player.x + cos(movingYaw.toRadians()) * 0.5) != floor(player.x) ||
                floor(player.z + sin(movingYaw.toRadians()) * 0.5) != floor(player.z)

            val posInDirection = player.pos.offset(Direction.fromRotation(movingYaw.toDouble()), 0.6).toBlockPos()

            val isLeaningOffBlock = player.blockPos.down().getState()?.isAir == true
            val nextBlockIsAir = posInDirection.down().getState()?.isAir == true

            if (isLeaningOffBlock && nextBlockIsAir) {
                isOnRightSide = !isOnRightSide
            }
        }

        val finalYaw = movingYaw + if (isOnRightSide) 45 else -45
        return Rotation(finalYaw, 75.7f)
    }

    private fun getRotationForDiagonalInput(movingYaw: Float): Rotation {
        return Rotation(movingYaw, 75.6f)
    }

    private fun getRotationForNoInput(target: BlockPlacementTarget): Rotation {
        val axisMovement = floor(target.rotation.yaw / 90) * 90

        val yaw = axisMovement + 45
        val pitch = 75f

        return Rotation(yaw, pitch)
    }

}
