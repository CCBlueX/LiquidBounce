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

fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
    val lhsLength = lhs.length
    val rhsLength = rhs.length

    if (lhsLength == 0) return rhsLength
    if (rhsLength == 0) return lhsLength

    // DP
    val cost = IntArray(lhsLength + 1) { it }

    for (i in 1..rhsLength) {
        var prevCost = cost[0]
        cost[0] = i

        for (j in 1..lhsLength) {
            val currentCost = cost[j]
            val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

            cost[j] = minOf(
                cost[j] + 1,
                cost[j - 1] + 1,
                prevCost + match
            )
            prevCost = currentCost
        }
    }

    return cost[lhsLength]
}
