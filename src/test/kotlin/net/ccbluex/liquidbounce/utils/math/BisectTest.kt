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

import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BisectTest {

    @Test
    fun `finds minimum for convex function inside interval`() {
        val (x, y) = findFunctionMinimumByBisect(-5.0, 10.0) { value ->
            (value - 2.0) * (value - 2.0) + 3.0
        }

        assertEquals(2.0, x, 1e-3)
        assertEquals(3.0, y, 1e-3)
    }

    @Test
    fun `finds minimum at lower boundary`() {
        val (x, y) = findFunctionMinimumByBisect(0.0, 10.0) { value ->
            value * value
        }

        assertEquals(0.0, x, 1e-3)
        assertEquals(0.0, y, 1e-6)
    }

    @Test
    fun `returns midpoint when interval is already smaller than minDelta`() {
        val (x, y) = findFunctionMinimumByBisect(1.0, 1.00001, minDelta = 1e-4) { value ->
            value * value + 1.0
        }

        assertEquals(1.000005, x, 1e-12)
        assertEquals(x * x + 1.0, y, 1e-12)
    }

    @Test
    fun `rejects descending intervals`() {
        assertThrows(IllegalArgumentException::class.java) {
            findFunctionMinimumByBisect(2.0, 1.0) { it }
        }
    }

    @Test
    fun `rejects non positive min delta`() {
        assertThrows(IllegalArgumentException::class.java) {
            findFunctionMinimumByBisect(0.0, 1.0, minDelta = 0.0) { it }
        }
    }
}
