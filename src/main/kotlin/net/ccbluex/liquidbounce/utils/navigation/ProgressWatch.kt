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

package net.ccbluex.liquidbounce.utils.navigation

/** Counts the ticks in a row a mover got no [minProgress] closer to its target than it has been. */
internal class ProgressWatch(private val minProgress: Double, private val stuckTicks: Int) {

    var idleTicks = 0
        private set

    private var bestDistance = Double.MAX_VALUE

    /** Whether the mover, now [distance] from its target, has gone [stuckTicks] ticks without progress. */
    fun stalls(distance: Double): Boolean {
        if (distance < bestDistance - minProgress) {
            bestDistance = distance
            idleTicks = 0
            return false
        }
        return ++idleTicks >= stuckTicks
    }

    /** Forgives the ticks without progress so far, for a wait on the way. */
    fun excuse() {
        idleTicks = 0
    }

    fun reset() {
        bestDistance = Double.MAX_VALUE
        idleTicks = 0
    }

}
