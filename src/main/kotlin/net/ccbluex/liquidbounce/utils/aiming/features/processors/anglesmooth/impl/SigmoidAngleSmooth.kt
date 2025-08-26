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
 *
 *
 */

package net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.FactorAngleSmooth
import net.ccbluex.liquidbounce.utils.kotlin.random
import kotlin.math.exp

@Deprecated("Interpolation mode combines Sigmoid and Bezier interpolation", ReplaceWith("InterpolationAngleSmooth"))
class SigmoidAngleSmooth(parent: ChoiceConfigurable<*>) : FactorAngleSmooth("Sigmoid", parent) {

    private val horizontalTurnSpeed by floatRange("HorizontalTurnSpeed", 180f..180f,
        0.0f..180f)
    private val verticalTurnSpeed by floatRange("VerticalTurnSpeed", 180f..180f,
        0.0f..180f)

    private val steepness by float("Steepness", 10f, 0.0f..20f)
    private val midpoint by float("Midpoint", 0.3f, 0.0f..1.0f)

    /**
     * Calculate the factors for the rotation towards the target rotation.
     *
     * @param currentRotation The current rotation
     * @param targetRotation The target rotation
     */
    override fun calculateFactors(
        rotationTarget: RotationTarget?,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Pair<Float, Float> {
        val rotationDifference = currentRotation.angleTo(targetRotation)

        val (horizontalTurnSpeed, verticalTurnSpeed) = if (rotationTarget != null) {
            horizontalTurnSpeed.random() to verticalTurnSpeed.random()
        } else {
            // Slowest turn speed, so we can calculate the slowest turn speed
            horizontalTurnSpeed.start to verticalTurnSpeed.start
        }

        val horizontalFactor = computeFactor(rotationDifference, horizontalTurnSpeed)
        val verticalFactor = computeFactor(rotationDifference, verticalTurnSpeed)

        return horizontalFactor to verticalFactor
    }

    private fun computeFactor(rotationDifference: Float, turnSpeed: Float): Float {
        val scaledDifference = rotationDifference / 120f
        val sigmoid = 1 / (1 + exp((-steepness * (scaledDifference - midpoint)).toDouble()))
        val interpolatedSpeed = sigmoid * turnSpeed

        return interpolatedSpeed.toFloat()
            .coerceAtLeast(0f)
            .coerceAtMost(180f)
    }

}
