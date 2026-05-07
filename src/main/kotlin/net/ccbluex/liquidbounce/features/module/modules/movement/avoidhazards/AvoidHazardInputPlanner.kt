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
package net.ccbluex.liquidbounce.features.module.modules.movement.avoidhazards

import net.ccbluex.liquidbounce.utils.math.toDegrees
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Picks a safe replacement input that stays as close to the original movement
 * direction as possible.
 */
object AvoidHazardInputPlanner {

    private data class Candidate(
        val input: DirectionalInput,
        val angle: Double
    )

    private val candidates = arrayOf(
        Candidate(DirectionalInput.FORWARDS, 0.0),
        Candidate(DirectionalInput.FORWARDS_RIGHT, 45.0),
        Candidate(DirectionalInput.RIGHT, 90.0),
        Candidate(DirectionalInput.BACKWARDS_RIGHT, 135.0),
        Candidate(DirectionalInput.BACKWARDS, 180.0),
        Candidate(DirectionalInput.BACKWARDS_LEFT, -135.0),
        Candidate(DirectionalInput.LEFT, -90.0),
        Candidate(DirectionalInput.FORWARDS_LEFT, -45.0)
    )

    fun chooseSafeInput(
        originalInput: DirectionalInput,
        isSafe: (DirectionalInput) -> Boolean
    ): DirectionalInput {
        if (!originalInput.isMoving) {
            return DirectionalInput.NONE
        }

        if (isSafe(originalInput)) {
            return originalInput
        }

        val sourceAngle = angleOf(originalInput) ?: return DirectionalInput.NONE

        return candidates
            .filter { it.input != originalInput }
            .sortedBy { angularDifference(sourceAngle, it.angle) }
            .firstNotNullOfOrNull { it.input.takeIf(isSafe) }
            ?: DirectionalInput.NONE
    }

    private fun angleOf(input: DirectionalInput): Double? {
        val x = axis(input.left, input.right)
        val z = axis(input.forwards, input.backwards)

        if (x == 0 && z == 0) {
            return null
        }

        return atan2(x.toDouble(), z.toDouble()).toDegrees()
    }

    private fun axis(positive: Boolean, negative: Boolean): Int {
        val positiveInt = if (positive) 1 else 0
        val negativeInt = if (negative) 1 else 0
        return positiveInt - negativeInt
    }

    private fun angularDifference(a: Double, b: Double): Double {
        return abs(Mth.wrapDegrees(a - b))
    }
}
