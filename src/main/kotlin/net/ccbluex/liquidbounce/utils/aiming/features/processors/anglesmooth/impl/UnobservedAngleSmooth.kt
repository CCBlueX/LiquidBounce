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
package net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.AngleSmooth

/**
 * Most anticheats only check the legitimacy of a rotation
 * for a period of time after a hit or during hit player's hurt time
 */
class UnobservedAngleSmooth(parent: ChoiceConfigurable<*>) : AngleSmooth("Unobserved", parent) {

    private var observedRotation = choices(this, "ObservedRotation") {
        arrayOf(
            MinaraiAngleSmooth(it, InterpolationAngleSmooth(it)),
            InterpolationAngleSmooth(it),
            SigmoidAngleSmooth(it),
            LinearAngleSmooth(it)
        )
    }

    private val observationTime by int("ObservationTime", 8, 1..300)

    override fun process(
        rotationTarget: RotationTarget,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Rotation = if (player.lastAttackedTicks >= observationTime) {
            targetRotation
        } else {
            observedRotation.activeChoice.process(rotationTarget, currentRotation, targetRotation)
        }

    override fun calculateTicks(
        currentRotation: Rotation,
        targetRotation: Rotation
    ) = if (player.lastAttackedTicks >= observationTime) {
        0
    } else {
        observedRotation.activeChoice.calculateTicks(currentRotation, targetRotation)
    }
}
