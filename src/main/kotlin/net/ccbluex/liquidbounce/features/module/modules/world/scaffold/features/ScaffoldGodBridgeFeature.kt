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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.util.math.Direction
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin

object ScaffoldGodBridgeFeature {
    private var isOnRightSide = false

    fun optimizeRotation(target: BlockPlacementTarget?): Rotation? {
        val dirInput = DirectionalInput(player.input)

        if (dirInput == DirectionalInput.NONE) {
            target ?: return null

            return getRotationForNoInput(target)
        }

        val direction = getMovementDirectionOfInput(player.yaw, dirInput) + 180

        // Round to 45°-steps (NORTH, NORTH_EAST, etc.)
        val movingYaw = round(direction / 45) * 45
        val isMovingStraight = movingYaw % 90 == 0f

        ScaffoldAutoJumpFeature.isGoingDiagonal = !isMovingStraight

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

            if (isLeaningOffBlock
                && nextBlockIsAir
            ) {
                isOnRightSide = !isOnRightSide
            }
        }

        val finalYaw = movingYaw + if (isOnRightSide) 45 else -45

        return Rotation(finalYaw, 75f)
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
