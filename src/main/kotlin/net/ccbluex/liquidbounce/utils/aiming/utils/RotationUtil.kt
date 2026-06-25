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
package net.ccbluex.liquidbounce.utils.aiming.utils

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationDelta
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_12_2
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.client.player.LocalPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity

fun LocalPlayer.setRotation(rotation: Rotation) {
    rotation.normalize().let { normalizedRotation ->
        xRotO = xRot
        yRotO = yRot
        yBob = yRot
        yBobO = yRot

        yRot = normalizedRotation.yaw
        xRot = normalizedRotation.pitch
    }
}

fun LocalPlayer.withFixedYaw(rotation: Rotation) = rotation.yaw + angleDifference(yRot, rotation.yaw)

object RotationUtil {

    private const val MOUSE_TURN_SCALE_FLOAT = 0.15f
    private const val MOUSE_TURN_SCALE_DOUBLE = 0.15

    val gcd: Double
        get() {
            val f = mc.options.sensitivity().get() * 0.6F.toDouble() + 0.2F.toDouble()
            return f * f * f * 8.0 * MOUSE_TURN_SCALE_DOUBLE
        }

    /**
     * Converts the values passed from `MouseHandler.turnPlayer` to the yaw/pitch delta applied by vanilla.
     *
     * [1.12.2 reference](https://github.com/WangTingZheng/mcp940/blob/d0c030a4139ce7cf3f284b180f0d9ea87bdf8141/src/minecraft/net/minecraft/entity/Entity.java#L479-L497)
     *
     * @see net.minecraft.world.entity.Entity.turn
     */
    fun mouseTurnDelta(cursorDeltaX: Double, cursorDeltaY: Double): RotationDelta {
        val deltaPitch: Float
        val deltaYaw: Float

        if (isOlderThanOrEqual1_12_2) {
            deltaPitch = (cursorDeltaY * MOUSE_TURN_SCALE_DOUBLE).toFloat()
            deltaYaw = (cursorDeltaX * MOUSE_TURN_SCALE_DOUBLE).toFloat()
        } else {
            deltaPitch = cursorDeltaY.toFloat() * MOUSE_TURN_SCALE_FLOAT
            deltaYaw = cursorDeltaX.toFloat() * MOUSE_TURN_SCALE_FLOAT
        }

        return RotationDelta(deltaYaw, deltaPitch)
    }

    fun applyMouseTurnDelta(rotation: Rotation, cursorDeltaX: Double, cursorDeltaY: Double): Rotation {
        val delta = mouseTurnDelta(cursorDeltaX, cursorDeltaY)

        return Rotation(
            yaw = rotation.yaw + delta.deltaYaw,
            pitch = (rotation.pitch + delta.deltaPitch).coerceIn(-90f, 90f)
        )
    }

    /**
     * Calculates the angle between the cross-hair and the entity.
     *
     * Useful for deciding if the player is looking at something or not.
     */
    fun crosshairAngleToEntity(entity: Entity): Float {
        val player = mc.player ?: return 0.0F
        val eyes = player.eyePosition

        val rotationToEntity = Rotation.lookingAt(point = entity.boundingBox.center, from = eyes)

        return player.rotation.angleTo(rotationToEntity)
    }

    /**
     * Calculate difference between two angle points
     */
    fun angleDifference(a: Float, b: Float) = Mth.wrapDegrees(a - b)
}
