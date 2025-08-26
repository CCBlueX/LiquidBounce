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

package net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation

abstract class FactorAngleSmooth(name: String, parent: ChoiceConfigurable<*>) : AngleSmooth(name, parent) {

    /**
     * Calculate the factors for the rotation towards the target rotation.
     *
     * @param currentRotation The current rotation
     * @param targetRotation The target rotation
     */
    abstract fun calculateFactors(
        rotationTarget: RotationTarget?,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Pair<Float, Float>

    override fun process(
        rotationTarget: RotationTarget,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Rotation {
        val (horizontalFactor, verticalFactor) = calculateFactors(rotationTarget, currentRotation, targetRotation)
        return currentRotation.towardsLinear(targetRotation, horizontalFactor, verticalFactor)
    }

    override fun calculateTicks(currentRotation: Rotation, targetRotation: Rotation): Int {
        var currentRotation = currentRotation
        var ticks = -1

        do {
            val (horizontalFactor, verticalFactor) = calculateFactors(null, currentRotation, targetRotation)

            currentRotation = currentRotation.towardsLinear(targetRotation, horizontalFactor, verticalFactor)
            ticks++
        } while (!currentRotation.approximatelyEquals(targetRotation) && ticks < 80)

        return ticks
    }

}
