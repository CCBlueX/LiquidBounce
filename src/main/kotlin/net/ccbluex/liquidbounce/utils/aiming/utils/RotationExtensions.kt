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
package net.ccbluex.liquidbounce.utils.aiming.utils

import net.ccbluex.liquidbounce.utils.aiming.data.Orientation
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.MathHelper

val gcd: Double
    get() {
        val f = mc.options.mouseSensitivity.value * 0.6F.toDouble() + 0.2F.toDouble()
        return f * f * f * 8.0 * 0.15F
    }

/**
 * Calculate difference between two angle points
 */
fun angleDifference(a: Float, b: Float) = MathHelper.wrapDegrees(a - b)

/**
 * Inverts yaw (-180 to 180)
 */
fun invertYaw(yaw: Float) = (yaw + 180) % 360

fun PlayerEntity?.applyRotation(rotation: Orientation) {
    this ?: return

    rotation.fixedSensitivity().let {
        prevPitch = pitch
        prevYaw = yaw

        yaw = it.yaw
        pitch = it.pitch
    }
}
