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
package net.ccbluex.liquidbounce.utils.aiming.data

import net.ccbluex.liquidbounce.utils.aiming.RotationObserver
import net.ccbluex.liquidbounce.utils.aiming.utils.angleDifference
import net.ccbluex.liquidbounce.utils.aiming.utils.gcd
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

data class Orientation(
    var yaw: Float,
    var pitch: Float
) {

    companion object {
        val ZERO = Orientation(0f, 0f)
    }

    val polar3d: Vec3d
        get() = Vec3d.fromPolar(pitch, yaw)

    /**
     * hypot(abs(angleDifference(a.yaw, b.yaw).toDouble()), abs((a.pitch - b.pitch).toDouble()))
     */
    fun differenceTo(orientation: Orientation): Double {
        val yawDiff = abs(angleDifference(yaw, orientation.yaw).toDouble())
        val pitchDiff = abs((pitch - orientation.pitch).toDouble())
        return hypot(yawDiff, pitchDiff)
            .coerceAtLeast(0.0)
    }

    /**
     * Fix rotation based on sensitivity
     */
    fun fixedSensitivity(): Orientation {
        val gcd = gcd

        // get previous rotation
        val orientation = RotationObserver.serverOrientation

        // get rotation differences
        val (deltaYaw, deltaPitch) = Orientation(yaw - orientation.yaw, pitch - orientation.pitch)

        // proper rounding
        val g1 = (deltaYaw / gcd).roundToInt() * gcd
        val g2 = (deltaPitch / gcd).roundToInt() * gcd

        // fix rotation
        val yaw = orientation.yaw + g1.toFloat()
        val pitch = orientation.pitch + g2.toFloat()

        return Orientation(yaw, pitch.coerceIn(-90f, 90f))
    }

}


