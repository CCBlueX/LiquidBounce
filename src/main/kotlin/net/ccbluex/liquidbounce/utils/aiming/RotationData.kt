/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.roundToInt

data class Rotation(var yaw: Float, var pitch: Float) {

    val rotationVec: Vec3d
        get() {
            val yawCos = MathHelper.cos(-yaw * 0.017453292f)
            val yawSin = MathHelper.sin(-yaw * 0.017453292f)
            val pitchCos = MathHelper.cos(pitch * 0.017453292f)
            val pitchSin = MathHelper.sin(pitch * 0.017453292f)
            return Vec3d((yawSin * pitchCos).toDouble(), (-pitchSin).toDouble(), (yawCos * pitchCos).toDouble())
        }

    /**
     * Fix rotation based on sensitivity
     */
    fun fixedSensitivity(): Rotation {
        val f = mc.options.mouseSensitivity.value * 0.6F.toDouble() + 0.2F.toDouble()
        val gcd = f * f * f * 8.0 * 0.15F

        // get previous rotation
        val rotation = RotationManager.serverRotation

        // get rotation differences
        val (deltaYaw, deltaPitch) = Rotation(yaw - rotation.yaw, pitch - rotation.pitch)

        // proper rounding
        val g1 = (deltaYaw / gcd).roundToInt() * gcd
        val g2 = (deltaPitch / gcd).roundToInt() * gcd

        // fix rotation
        val yaw = rotation.yaw + g1.toFloat()
        val pitch = rotation.pitch + g2.toFloat()

        return Rotation(yaw, pitch.coerceIn(-90f, 90f))
    }

}

data class VecRotation(val rotation: Rotation, val vec: Vec3d)
