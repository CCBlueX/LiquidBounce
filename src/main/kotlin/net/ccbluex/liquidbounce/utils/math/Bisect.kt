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
package net.ccbluex.liquidbounce.utils.math

import it.unimi.dsi.fastutil.doubles.DoubleDoublePair

/**
 * Finds the minimum between min and max.
 */
inline fun findFunctionMinimumByBisect(
    from: Double,
    to: Double,
    minDelta: Double = 1E-4,
    function: (Double) -> Double
): DoubleDoublePair {
    var lowerBound = from
    var upperBound = to

    var t = 0

    while (upperBound - lowerBound > minDelta) {
        val mid = (lowerBound + upperBound) / 2

        val leftValue = function((lowerBound + mid) / 2)
        val rightValue = function((mid + upperBound) / 2)

        if (leftValue < rightValue) {
            upperBound = mid
        } else {
            lowerBound = mid
        }

        t++
    }

    val x = (lowerBound + upperBound) / 2
    val y = function(x)

    return DoubleDoublePair.of(x, y)
}
