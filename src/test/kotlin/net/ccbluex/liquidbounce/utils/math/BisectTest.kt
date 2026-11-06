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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.Test

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
        assertFailsWith<IllegalArgumentException> {
            findFunctionMinimumByBisect(2.0, 1.0) { it }
        }
    }

    @Test
    fun `rejects non positive min delta`() {
        assertFailsWith<IllegalArgumentException> {
            findFunctionMinimumByBisect(0.0, 1.0, minDelta = 0.0) { it }
        }
    }

    @Test
    fun `stops when adjacent bounds cannot be bisected`() {
        val from = 1.0
        val to = Math.nextUp(from)
        var evaluations = 0

        val (x, y) = findFunctionMinimumByBisect(from, to, minDelta = Double.MIN_VALUE) {
            assertTrue(++evaluations <= 16, "Search must stop at floating-point precision")
            assertTrue(it in from..to)
            it
        }

        assertTrue(x in from..to)
        assertEquals(x, y)
    }

    @Test
    fun `terminates when tolerance is smaller than representable precision`() {
        var evaluations = 0

        val (x, y) = findFunctionMinimumByBisect(1.0, 2.0, minDelta = 1e-20) {
            assertTrue(++evaluations <= 256, "Search must keep making progress")
            it
        }

        assertEquals(1.0, x, Math.ulp(1.0))
        assertEquals(x, y)
    }

    @Test
    fun `keeps samples finite in a large positive interval`() {
        assertLargeIntervalMinimum(Double.MAX_VALUE * 0.5, Double.MAX_VALUE, 0.75)
    }

    @Test
    fun `keeps samples finite in a large negative interval`() {
        assertLargeIntervalMinimum(-Double.MAX_VALUE, -Double.MAX_VALUE * 0.5, -0.75)
    }

    @Test
    fun `handles an interval whose width overflows`() {
        assertLargeIntervalMinimum(-Double.MAX_VALUE, Double.MAX_VALUE, 0.0)
    }

    @Test
    fun `preserves a large single point interval`() {
        val (x, y) = findFunctionMinimumByBisect(Double.MAX_VALUE, Double.MAX_VALUE) {
            assertEquals(Double.MAX_VALUE, it)
            42.0
        }

        assertEquals(Double.MAX_VALUE, x)
        assertEquals(42.0, y)
    }

    @Test
    fun `preserves a subnormal single point interval`() {
        val (x, y) = findFunctionMinimumByBisect(Double.MIN_VALUE, Double.MIN_VALUE) { it }

        assertEquals(Double.MIN_VALUE, x)
        assertEquals(Double.MIN_VALUE, y)
    }

    private fun assertLargeIntervalMinimum(from: Double, to: Double, expected: Double) {
        var evaluations = 0
        val (x, y) = findFunctionMinimumByBisect(from, to, minDelta = 1e290) {
            assertTrue(++evaluations <= 256, "Search must converge for finite bounds")
            assertTrue(it.isFinite() && it in from..to, "Sample outside finite interval: $it")
            val difference = it / Double.MAX_VALUE - expected
            difference * difference
        }

        assertEquals(expected, x / Double.MAX_VALUE, 1e-14)
        assertEquals(0.0, y, 1e-28)
    }
}
