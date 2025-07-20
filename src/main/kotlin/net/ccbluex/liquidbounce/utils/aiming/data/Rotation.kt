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
package net.ccbluex.liquidbounce.utils.aiming.data

import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class Rotation(
    var yaw: Float,
    var pitch: Float,
    var isNormalized: Boolean = false
) {

    companion object {
        val ZERO = Rotation(0f, 0f)

        fun lookingAt(point: Vec3d, from: Vec3d): Rotation {
            return fromRotationVec(point.subtract(from))
        }

        fun fromRotationVec(lookVec: Vec3d): Rotation {
            val diffX = lookVec.x
            val diffY = lookVec.y
            val diffZ = lookVec.z

            return Rotation(
                MathHelper.wrapDegrees(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
                MathHelper.wrapDegrees((-Math.toDegrees(atan2(diffY, sqrt(diffX * diffX + diffZ * diffZ)))).toFloat())
            )
        }
    }

    val directionVector: Vec3d
        get() = Vec3d.fromPolar(pitch, yaw)

    /**
     * Fixes GCD and Modulo 360° at yaw
     *
     * @return [Rotation] with fixed yaw and pitch
     */
    fun normalize(): Rotation {
        if (isNormalized) return this

        val gcd = RotationUtil.gcd

        // We use the [currentRotation] to calculate the normalized rotation, if it's null, we use
        // the player's rotation
        val currentRotation = RotationManager.currentRotation ?: player.rotation

        // get rotation differences
        val diff = currentRotation.rotationDeltaTo(this)

        // proper rounding
        val g1 = (diff.deltaYaw / gcd).roundToInt() * gcd
        val g2 = (diff.deltaPitch / gcd).roundToInt() * gcd

        // fix rotation
        val yaw = currentRotation.yaw + g1.toFloat()
        val pitch = currentRotation.pitch + g2.toFloat()

        return Rotation(yaw, pitch.coerceIn(-90f, 90f), isNormalized = true)
    }

    /**
     * Calculates the angle between this and the other rotation.
     *
     * @return angle in degrees
     */
    fun angleTo(other: Rotation): Float {
        return rotationDeltaTo(other).length().coerceAtMost(180.0F)
    }

    /**
     * Calculates what angles would need to be added to arrive at [other].
     *
     * Wrapped 360°
     */
    fun rotationDeltaTo(other: Rotation): RotationDelta {
        return RotationDelta(
            angleDifference(other.yaw, this.yaw),
            angleDifference(other.pitch, this.pitch)
        )
    }

    /**
     * Calculates a new rotation that is closer to the [other] rotation by a limiting factor of
     * [horizontalFactor] and [verticalFactor], which should be between 0 and 180 degrees.
     */
    fun towardsLinear(other: Rotation, horizontalFactor: Float, verticalFactor: Float): Rotation {
        val diff = rotationDeltaTo(other)
        val rotationDifference = diff.length()
        val straightLineYaw = abs(diff.deltaYaw / rotationDifference) * horizontalFactor
        val straightLinePitch = abs(diff.deltaPitch / rotationDifference) * verticalFactor

        return Rotation(
            this.yaw + diff.deltaYaw.coerceIn(-straightLineYaw, straightLineYaw),
            this.pitch + diff.deltaPitch.coerceIn(-straightLinePitch, straightLinePitch)
        )
    }

    fun approximatelyEquals(other: Rotation, tolerance: Float = 2f): Boolean {
        return angleTo(other) <= tolerance
    }

}


