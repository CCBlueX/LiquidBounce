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
 */
package net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.FactorAngleSmooth
import kotlin.math.abs
import kotlin.math.exp

class InterpolationAngleSmooth(
    parent: ChoiceConfigurable<*>,
    horizontalSpeed: IntRange = 80..85,
    verticalSpeed: IntRange = 20..25,
    directionChangeFactor: IntRange = 95..100,
) : FactorAngleSmooth("Interpolation", parent) {

    private val horizontalSpeed by intRange("HorizontalSpeed", horizontalSpeed, 1..100, "%")
    private val verticalSpeed by intRange("VerticalSpeed", verticalSpeed, 1..100, "%")
    private val directionChangeFactor by intRange("DirectionChangeFactor", directionChangeFactor, 0..100, "%")

    private val midpoint by float("Midpoint", 0.35f, 0.0f..1.0f)

    private class Sigmoid {
        fun transform(t: Float): Float {
            return 1f / (1f + exp(-0.5f * (t - 0.3f)))
        }
    }

    private class Bezier {
        fun transform(start: Float, end: Float, t: Float): Float {
            return (1f - t) * (1f - t) * start + 2f * (1f - t) * t * 1f + t * t * end
        }
    }

    private val sigmoid = Sigmoid()
    private val bezier = Bezier()

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
        val (yawDiff, pitchDiff) = currentRotation.rotationDeltaTo(targetRotation)
        ModuleDebug.debugParameter(this, "Yaw Diff", yawDiff)
        ModuleDebug.debugParameter(this, "Pitch Diff", pitchDiff)

        val directionChange = RotationManager.previousRotationTarget.takeIf { rotationTarget != null }?.run {
            rotation.angleTo(targetRotation).coerceIn(0f, 1f) * (directionChangeFactor.random().toFloat() / 100.0f)
        } ?: 0f
        ModuleDebug.debugParameter(this, "Direction Change", directionChange)

        val horizontalSpeed = if (rotationTarget != null) {
            horizontalSpeed.random()
        } else {
            horizontalSpeed.start
        }.toFloat() / 100.0f

        val verticalSpeed = if (rotationTarget != null) {
            verticalSpeed.random()
        } else {
            verticalSpeed.start
        }.toFloat() / 100.0f

        ModuleDebug.debugParameter(this, "Horizontal Speed", horizontalSpeed)
        ModuleDebug.debugParameter(this, "Vertical Speed", verticalSpeed)

        val horizontalFactor = calculateFactor("Yaw", abs(yawDiff), horizontalSpeed.coerceIn(0f, 1f),
            directionChange)
        val verticalFactor = calculateFactor("Pitch", abs(pitchDiff), verticalSpeed.coerceIn(0f, 1f),
            directionChange)

        // Multiplying the factor with the difference in yaw and pitch allows us
        // to bypass the linear [towardsLinear] method
        return horizontalFactor * abs(yawDiff) to verticalFactor * abs(pitchDiff)
    }

    private fun calculateFactor(name: String, rotationDifference: Float, turnSpeed: Float,
                                directionChange: Float): Float {
        val t = (rotationDifference / 180f).coerceIn(0f, 1f)
        ModuleDebug.debugParameter(this, "$name T", t)

        val bezierSpeed = bezier.transform(0.05f, 1f, 1f - t)
        val sigmoidSpeed = sigmoid.transform(t)

        ModuleDebug.debugParameter(this, "$name Bezier", bezierSpeed)
        ModuleDebug.debugParameter(this, "$name Sigmoid", sigmoidSpeed)

        return if (t > midpoint) {
            ModuleDebug.debugParameter(this, "$name R", "Bezier")
            bezierSpeed * turnSpeed
        } else {
            ModuleDebug.debugParameter(this, "$name R", "Sigmoid")
            sigmoidSpeed * (turnSpeed + directionChange).coerceIn(0f, 1f)
        }
    }

}
