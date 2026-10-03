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
package net.ccbluex.liquidbounce.utils.math

import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction
import it.unimi.dsi.fastutil.doubles.DoubleDoublePair

/**
 * Finds the minimum of a unimodal function between [from] and [to].
 * Stops at [minDelta] or when floating-point precision prevents further bisection.
 */
fun findFunctionMinimumByBisect(
    from: Double,
    to: Double,
    minDelta: Double = 1E-4,
    function: Double2DoubleFunction,
): DoubleDoublePair {
    require(from.isFinite() && to.isFinite()) { "Search interval must be finite" }
    require(from <= to) { "Search interval must satisfy from <= to" }
    require(minDelta.isFinite() && minDelta > 0.0) { "minDelta must be finite and greater than 0" }

    var lowerBound = from
    var upperBound = to

    while (upperBound - lowerBound > minDelta) {
        val mid = bisectMidpoint(lowerBound, upperBound)
        if (mid == lowerBound || mid == upperBound) {
            break
        }

        val leftValue = function.get(bisectMidpoint(lowerBound, mid))
        val rightValue = function.get(bisectMidpoint(mid, upperBound))

        if (leftValue < rightValue) {
            upperBound = mid
        } else {
            lowerBound = mid
        }
    }

    val x = bisectMidpoint(lowerBound, upperBound)
    val y = function.get(x)

    return DoubleDoublePair.of(x, y)
}

private fun bisectMidpoint(from: Double, to: Double): Double {
    val width = to - from
    return if (width.isFinite()) from + width * 0.5 else from * 0.5 + to * 0.5
}
