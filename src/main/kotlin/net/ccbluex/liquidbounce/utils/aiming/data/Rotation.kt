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
package net.ccbluex.liquidbounce.utils.aiming.data

import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.math.toDegrees
import net.ccbluex.liquidbounce.utils.math.toRadians
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import java.lang.Math.fma
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

@JvmRecord
data class Rotation @JvmOverloads constructor(
    val yaw: Float,
    val pitch: Float,
    val isNormalized: Boolean = false,
) {

    companion object {
        @JvmField
        val ZERO = Rotation(0f, 0f)

        @JvmStatic
        fun lookingAt(point: Vec3, from: Vec3): Rotation {
            return fromRotationVec(point.subtract(from))
        }

        @JvmStatic
        fun fromRotationVec(lookVec: Vec3): Rotation =
            fromRotationVec(lookVec.x, lookVec.y, lookVec.z)

        @JvmStatic
        fun fromRotationVec(diffX: Double, diffY: Double, diffZ: Double): Rotation {
            return Rotation(
                yaw = Mth.wrapDegrees(atan2(diffZ, diffX).toDegrees().toFloat() - 90f),
                pitch = Mth.wrapDegrees(-atan2(diffY, hypot(diffX, diffZ)).toDegrees().toFloat()),
            )
        }
    }

    val directionVector: Vec3
        get() = Vec3.directionFromRotation(pitch, yaw)

    val xRot: Float get() = pitch
    val yRot: Float get() = yaw

    @JvmOverloads
    fun toQuaternion(dest: Quaternionf = Quaternionf()): Quaternionf =
        dest.rotationYXZ(Mth.PI - yRot.toRadians(), -xRot.toRadians(), 0f)

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
     * Calculates the great-circle angle between the two view directions.
     *
     * This intentionally ignores differences that do not change the forward vector, such as yaw
     * at a vertical pitch. Use [rotationDeltaLengthTo] for mouse movement, smoothing and rotation
     * state comparisons.
     *
     * @return direction angle in degrees
     */
    fun directionAngleTo(other: Rotation): Float {
        val direction = directionVector
        val otherDirection = other.directionVector

        return atan2(
            direction.cross(otherDirection).length(),
            direction.dot(otherDirection)
        ).toDegrees().toFloat()
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
     * Calculates the Euclidean length of the wrapped yaw/pitch control delta.
     *
     * Unlike [directionAngleTo], this preserves yaw differences at vertical pitches and therefore
     * matches Minecraft's independent mouse, packet and movement rotation axes.
     */
    fun rotationDeltaLengthTo(other: Rotation): Float = rotationDeltaTo(other).length()

    /**
     * Calculates a new rotation that is closer to the [other] rotation by a limiting factor of
     * [horizontalFactor] and [verticalFactor], which should be between 0 and 180 degrees.
     */
    fun towardsLinear(other: Rotation, horizontalFactor: Float, verticalFactor: Float): Rotation {
        val diff = rotationDeltaTo(other)
        val rotationDifference = diff.length()
        val straightLineYaw = abs(diff.deltaYaw / rotationDifference) * horizontalFactor
        val straightLinePitch = abs(diff.deltaPitch / rotationDifference) * verticalFactor

        return this.add(
            y = diff.deltaYaw.coerceIn(-straightLineYaw, straightLineYaw),
            x = diff.deltaPitch.coerceIn(-straightLinePitch, straightLinePitch),
        )
    }

    /**
     * Interpolates this rotation towards [other] using the given [factor].
     */
    fun interpolateTo(other: Rotation, factor: Float): Rotation = Rotation(
        fma(factor, other.yaw - yaw, yaw),
        fma(factor, other.pitch - pitch, pitch),
    )

    @JvmOverloads
    fun isDirectionCloseTo(other: Rotation, tolerance: Float = 2f): Boolean =
        directionAngleTo(other) <= tolerance

    @JvmOverloads
    fun isRotationDeltaCloseTo(other: Rotation, tolerance: Float = 2f): Boolean =
        rotationDeltaLengthTo(other) <= tolerance

    fun add(x: Float, y: Float): Rotation {
        return Rotation(yaw = this.yRot + y, pitch = this.xRot + x)
    }

}
