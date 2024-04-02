/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.utils.aiming.angleSmooth

import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min

class SigmoidAngleSmoothMode(override val parent: ChoiceConfigurable<*>) : AngleSmoothMode("Sigmoid") {

    private val horizontalTurnSpeed by floatRange("HorizontalTurnSpeed", 180f..180f,
        0.0f..180f)
    private val verticalTurnSpeed by floatRange("VerticalTurnSpeed", 180f..180f,
        0.0f..180f)

    private val steepness by float("Steepness", 10f, 0.0f..20f)
    private val midpoint by float("Midpoint", 0.3f, 0.0f..1.0f)

    override fun limitAngleChange(currentRotation: Rotation, targetRotation: Rotation,
                                  vec3d: Vec3d?,
                                  entity: Entity?): Rotation {
        val yawDifference = RotationManager.angleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDifference = RotationManager.angleDifference(targetRotation.pitch, currentRotation.pitch)

        val rotationDifference = hypot(abs(yawDifference), abs(pitchDifference))
        val (factorH, factorV) =
            computeFactor(rotationDifference, horizontalTurnSpeed.random()) to
            computeFactor(rotationDifference, verticalTurnSpeed.random())

        val straightLineYaw = abs(yawDifference / rotationDifference) * factorH
        val straightLinePitch = abs(pitchDifference / rotationDifference) * factorV

        return Rotation(
            currentRotation.yaw + yawDifference.coerceIn(-straightLineYaw, straightLineYaw),
            currentRotation.pitch + pitchDifference.coerceIn(-straightLinePitch, straightLinePitch)
        )
    }

    override fun howLongToReach(currentRotation: Rotation, targetRotation: Rotation): Int {
        val yawDifference = RotationManager.angleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDifference = RotationManager.angleDifference(targetRotation.pitch, currentRotation.pitch)

        val rotationDifference = hypot(abs(yawDifference), abs(pitchDifference))
        val (factorH, factorV) =
            computeFactor(rotationDifference, horizontalTurnSpeed.random()) to
            computeFactor(rotationDifference, verticalTurnSpeed.random())

        val straightLineYaw = abs(yawDifference / rotationDifference) * factorH
        val straightLinePitch = abs(pitchDifference / rotationDifference) * factorV

        return (rotationDifference / min(straightLineYaw, straightLinePitch)).toInt()
    }

    private fun computeFactor(rotationDifference: Float, turnSpeed: Double): Float {
        // Scale the rotation difference to fit within a reasonable range
        val scaledDifference = rotationDifference / 120f

        // Compute the sigmoid function
        val sigmoid = 1 / (1 + exp((-steepness * (scaledDifference - midpoint)).toDouble()))

        // Interpolate sigmoid value to fit within the range of turnSpeed
        val interpolatedSpeed = sigmoid * turnSpeed

        return interpolatedSpeed.toFloat()
            .coerceAtLeast(0f)
            .coerceAtMost(180f)
    }

}
