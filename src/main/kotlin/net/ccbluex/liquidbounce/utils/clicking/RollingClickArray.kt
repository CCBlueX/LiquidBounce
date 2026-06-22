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

package net.ccbluex.liquidbounce.utils.clicking

/**
 * A circular buffer that maintains double the cycle length and regenerates the second half
 * when reaching the midpoint
 */
class RollingClickArray(
    private val cycleLength: Int,
    val iterations: Int,
) {

    internal val array = IntArray(cycleLength * iterations)
    var head = 0
        private set
    private val size get() = array.size

    /**
     * Gets value at relative index from current head
     */
    fun get(relativeIndex: Int): Int {
        val actualIndex = (head + relativeIndex) % size
        return array[actualIndex]
    }

    /**
     * Sets value at relative index from current head
     */
    fun set(relativeIndex: Int, value: Int) {
        val actualIndex = (head + relativeIndex) % size
        array[actualIndex] = value
    }

    /**
     * Advances the head position and returns true if halfway point reached
     */
    fun advance(amount: Int = 1): Boolean {
        head = (head + amount) % size
        return head % cycleLength == 0
    }

    /**
     * Clears the array
     */
    fun clear() {
        array.fill(0)
        head = 0
    }

    fun push(cycleArray: IntArray) {
        require(cycleArray.size == cycleLength) { "Array size must match cycle length" }

        when (head) {
            0 -> System.arraycopy(cycleArray, 0, array, cycleLength, cycleLength)
            cycleLength -> System.arraycopy(cycleArray, 0, array, 0, cycleLength)
            else -> error("Head must be at 0 or cycle length")
        }
    }

}
