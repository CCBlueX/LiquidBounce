/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.getTargetedPosition
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.rawInput
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.LedgeAction
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldLedgeExtension
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique.NORMAL_INVESTIGATION_OFFSETS
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
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
import kotlin.random.Random

object ScaffoldGodBridgeTechnique : ScaffoldTechnique("GodBridge"), ScaffoldLedgeExtension {

    private enum class Mode(override val choiceName: String) : NamedChoice {
        JUMP("Jump"),
        SNEAK("Sneak"),
        /**
         * Might not be as consistent as the other modes.
         */
        STOP_INPUT("StopInput"),
        BACKWARDS("Backwards"),
        RANDOM("Random")
    }

    private var mode by enumChoice("Mode", Mode.JUMP)
    private var forceSneakBelowCount by int("ForceSneakBelowCount", 3, 0..10)
    private val sneakTime by intRange("SneakTime", 1..1, 1..10)

    override fun ledge(
        target: BlockPlacementTarget?,
        rotation: Rotation
    ): LedgeAction {
        if (!isSelected) {
            return LedgeAction.NO_LEDGE
        }

        val simulatedPlayerCache = PlayerSimulationCache.getSimulationForLocalPlayer()

        // Check if the current rotation is capable of placing a block on the next tick position,
        // this might be inconsistent when the rotation changes on the next tick as well,
        // but we hope it does not. :)
        val snapshotOne = simulatedPlayerCache.getSnapshotAt(1)

        ModuleDebug.debugParameter(this, "Snapshot Ledged", snapshotOne.clipLedged)

        return if (snapshotOne.clipLedged) {
            val cameraPosition = snapshotOne.pos.add(0.0, player.standingEyeHeight.toDouble(), 0.0)
            val currentCrosshairTarget = raycast(start = cameraPosition, direction = rotation.directionVector)

            if (target == null) {
                return LedgeAction.NO_LEDGE
            }

            val targetFullfillsRequirements = target.doesCrosshairTargetFullFillRequirements(currentCrosshairTarget)
            val isValidCrosshairTarget = ModuleScaffold.isValidCrosshairTarget(currentCrosshairTarget)

            ModuleDebug.debugParameter(this, "targetFullfillsRequirements", targetFullfillsRequirements.toString())
            ModuleDebug.debugParameter(this, "isValidCrosshairTarget", isValidCrosshairTarget.toString())

            // Does the crosshair target meet the requirements?
            if (targetFullfillsRequirements && isValidCrosshairTarget) {
                return LedgeAction.NO_LEDGE
            }

            // If the crosshair target does not meet the requirements,
            // we need to prevent the player from falling off the ledge e.g by jumping or sneaking.
            when {
                ModuleScaffold.blockCount < forceSneakBelowCount -> {
                    LedgeAction(sneakTime = sneakTime.random())
                }
                mode == Mode.JUMP -> LedgeAction(jump = true)
                mode == Mode.SNEAK -> LedgeAction(sneakTime = sneakTime.random())
                mode == Mode.STOP_INPUT -> LedgeAction(stopInput = true)
                mode == Mode.BACKWARDS -> LedgeAction(stepBack = true)
                mode == Mode.RANDOM -> if (Random.nextBoolean()) {
                    LedgeAction(jump = true, sneakTime = 0)
                } else {
                    LedgeAction(jump = false, sneakTime = sneakTime.random())
                }
                else -> LedgeAction.NO_LEDGE
            }
        } else {
            LedgeAction.NO_LEDGE
        }
    }

    private var isOnRightSide = false

    override fun findPlacementTarget(
        predictedPos: Vec3d,
        predictedPose: EntityPose,
        optimalLine: Line?,
        bestStack: ItemStack
    ): BlockPlacementTarget? {
        val searchOptions = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(
                NORMAL_INVESTIGATION_OFFSETS,
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            ),
            FaceHandlingOptions(CenterTargetPositionFactory),
            stackToPlaceWith = bestStack,
            PlayerLocationOnPlacement(position = predictedPos, pose = predictedPose),
        )

        return findBestBlockPlacementTarget(getTargetedPosition(predictedPos.toBlockPos()), searchOptions)
    }

    override fun getRotations(target: BlockPlacementTarget?): Rotation? {
        if (rawInput == DirectionalInput.NONE) {
            target ?: return null

            return getRotationForNoInput(target)
        }

        val direction = getMovementDirectionOfInput(player.yaw, rawInput) + 180

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

            val posInDirection = player.pos.offset(Direction.fromHorizontalDegrees(movingYaw.toDouble()), 0.6)
                .toBlockPos()

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
