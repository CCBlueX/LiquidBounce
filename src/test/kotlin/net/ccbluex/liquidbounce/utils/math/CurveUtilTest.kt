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

import org.joml.Vector2f
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CurveUtilTest {

    private val tolerance = 1e-5f

    @Test
    fun `transform returns first Y when xPos is less than first X`() {
        val data = listOf(
            Vector2f(0f, 10f),
            Vector2f(1f, 20f),
            Vector2f(2f, 30f)
        )
        val result = CurveUtil.transform(data, -1f, 0.5f)
        assertEquals(10f, result, tolerance)
    }

    @Test
    fun `transform returns last Y when xPos is greater than last X`() {
        val data = listOf(
            Vector2f(0f, 10f),
            Vector2f(1f, 20f),
            Vector2f(2f, 30f)
        )
        val result = CurveUtil.transform(data, 3f, 0.5f)
        assertEquals(30f, result, tolerance)
    }

    @Test
    fun `transform interpolates between points`() {
        val data = listOf(
            Vector2f(0f, 0f),
            Vector2f(1f, 10f),
            Vector2f(2f, 20f)
        )
        val result = CurveUtil.transform(data, 0.5f, 0.5f)
        assert(result in 0f..10f) { "Expected result between 0 and 10, got $result" }
    }

    @Test
    fun `transform returns exact Y when xPos equals a data point X`() {
        val data = listOf(
            Vector2f(0f, 0f),
            Vector2f(1f, 10f),
            Vector2f(2f, 20f)
        )
        val result = CurveUtil.transform(data, 1f, 0.5f)
        assertEquals(10f, result, tolerance)
    }

    @Test
    fun `transform handles single point list`() {
        val data = listOf(Vector2f(0f, 42f))
        val result1 = CurveUtil.transform(data, -1f, 0.5f)
        val result2 = CurveUtil.transform(data, 0f, 0.5f)
        val result3 = CurveUtil.transform(data, 1f, 0.5f)

        assertEquals(42f, result1, tolerance)
        assertEquals(42f, result2, tolerance)
        assertEquals(42f, result3, tolerance)
    }
}

