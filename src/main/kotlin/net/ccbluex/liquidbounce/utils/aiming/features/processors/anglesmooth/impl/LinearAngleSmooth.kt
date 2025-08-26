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

class LinearAngleSmooth(
    parent: ChoiceConfigurable<*>,
    horizontalTurnSpeed: ClosedFloatingPointRange<Float> = 180f..180f,
    verticalTurnSpeed: ClosedFloatingPointRange<Float> = 180f..180f,
) : FactorAngleSmooth("Linear", parent) {

    private val horizontalTurnSpeed by floatRange("HorizontalTurnSpeed", horizontalTurnSpeed,
        0.0f..180f)
    private val verticalTurnSpeed by floatRange("VerticalTurnSpeed", verticalTurnSpeed,
        0.0f..180f)

    override fun calculateFactors(
        rotationTarget: RotationTarget?,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Pair<Float, Float> {
        return if (rotationTarget != null) {
            horizontalTurnSpeed.random() to verticalTurnSpeed.random()
        } else {
            // Slowest turn speed, so we can calculate the slowest turn speed
            horizontalTurnSpeed.start to verticalTurnSpeed.start
        }
    }

}
