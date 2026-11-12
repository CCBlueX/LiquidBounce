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

package net.ccbluex.liquidbounce.utils.math.geometry

/**
 * Walks a `countA x countB` grid of cell centres: the point at index `i` sits at `(i + 0.5) / count`, so no
 * point ever lands on the border of the area being sampled.
 *
 * Every sampler that derives its point counts goes through this, which keeps the rounding and the
 * half-cell offset in one place.
 *
 * [action] returns `false` to stop the walk.
 *
 * @return `true` when every cell was visited, `false` when [action] stopped the walk
 */
@PublishedApi
internal inline fun forEachGridCenter(
    countA: Int,
    countB: Int,
    action: (a: Double, b: Double) -> Boolean,
): Boolean {
    require(countA > 0 && countB > 0) { "counts must be positive, were $countA and $countB" }

    for (i in 0 until countA) {
        val a = (i + 0.5) / countA
        for (j in 0 until countB) {
            val b = (j + 0.5) / countB
            if (!action(a, b)) {
                return false
            }
        }
    }

    return true
}
