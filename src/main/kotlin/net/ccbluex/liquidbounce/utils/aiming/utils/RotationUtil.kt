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
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.box
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

    val gcd: Double
        get() {
            val f = mc.options.sensitivity().get() * 0.6F.toDouble() + 0.2F.toDouble()
            return f * f * f * 8.0 * 0.15F
        }

    /**
     * Calculates the angle between the cross-hair and the entity.
     *
     * Useful for deciding if the player is looking at something or not.
     */
    fun crosshairAngleToEntity(entity: Entity): Float {
        val player = mc.player ?: return 0.0F
        val eyes = player.eyePosition

        val rotationToEntity = Rotation.lookingAt(point = entity.box.center, from = eyes)

        return player.rotation.angleTo(rotationToEntity)
    }

    /**
     * Calculate difference between two angle points
     */
    fun angleDifference(a: Float, b: Float) = Mth.wrapDegrees(a - b)
}
