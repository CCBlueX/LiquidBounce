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
 * Finds the minimum between min and max.
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
        val mid = (lowerBound + upperBound) * 0.5

        val leftValue = function.get((lowerBound + mid) * 0.5)
        val rightValue = function.get((mid + upperBound) * 0.5)

        if (leftValue < rightValue) {
            upperBound = mid
        } else {
            lowerBound = mid
        }
    }

    val x = (lowerBound + upperBound) * 0.5
    val y = function.get(x)

    return DoubleDoublePair.of(x, y)
}
