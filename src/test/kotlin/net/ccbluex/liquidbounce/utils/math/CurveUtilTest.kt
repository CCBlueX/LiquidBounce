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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CurveUtilTest {

    private companion object {
        const val EPSILON = 1e-5f
    }

    @Test
    fun `transform throws on empty input`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            CurveUtil.transform(emptyList(), 0f, 0.5f)
        }
        assertTrue(exception.message!!.contains("must not be empty"))
    }

    @Test
    fun `transform returns same value for single point`() {
        val data = listOf(5f to 42f)

        assertTransformY(42f, data, -1f, 0.5f)
        assertTransformY(42f, data, 5f, 0.5f)
        assertTransformY(42f, data, 100f, 0.5f)
    }

    @Test
    fun `transform clamps to first and last point`() {
        val data = listOf(0f to 10f, 1f to 20f, 2f to 30f)

        assertTransformY(10f, data, -1f, 0.5f)
        assertTransformY(30f, data, 3f, 0.5f)
    }

    @Test
    fun `transform extends left and right when out-of-bounds mode is extend`() {
        val data = listOf(0f to 0f, 1f to 10f, 2f to 30f)

        assertTransformY(-10f, data, -1f, 0.5f, CurveUtil.OnOutOfBounds.EXTEND)
        assertTransformY(50f, data, 3f, 0.5f, CurveUtil.OnOutOfBounds.EXTEND)
    }

    @Test
    fun `transform extends with unsorted input`() {
        val sorted = listOf(0f to 0f, 1f to 10f, 2f to 30f)
        val unsorted = listOf(2f to 30f, 0f to 0f, 1f to 10f)

        val expected = CurveUtil.transform(sorted, 3f, 0.4f, CurveUtil.OnOutOfBounds.EXTEND)
        val actual = CurveUtil.transform(unsorted, 3f, 0.4f, CurveUtil.OnOutOfBounds.EXTEND)
        assertEquals(expected, actual, EPSILON)
    }

    @Test
    fun `transform returns exact y for exact x match`() {
        val data = listOf(0f to 0f, 1f to 10f, 2f to 20f)
        assertTransformY(10f, data, 1f, 0.5f)
    }

    @Test
    fun `transform returns deterministic spline value for sample input`() {
        val data = listOf(0f to 0f, 1f to 10f, 2f to 20f)

        assertTransformY(5f, data, 0.5f, 0.5f)
        assertTransformY(16.875f, data, 1.5f, 0.5f)
    }

    @Test
    fun `transform handles two-point data with linear interpolation`() {
        val data = listOf(0f to 0f, 10f to 100f)

        val y1 = CurveUtil.transform(data, 2.5f, 0.5f)
        val y2 = CurveUtil.transform(data, 5f, 0.5f)
        val y3 = CurveUtil.transform(data, 7.5f, 0.5f)

        assertEquals(25f, y1, EPSILON)
        assertEquals(50f, y2, EPSILON)
        assertEquals(75f, y3, EPSILON)
    }

    @Test
    fun `transform normalizes unsorted input`() {
        val sorted = listOf(0f to 0f, 1f to 10f, 2f to 20f)
        val unsorted = listOf(2f to 20f, 0f to 0f, 1f to 10f)

        val xPos = 0.75f
        val expected = CurveUtil.transform(sorted, xPos, 0.4f)
        val actual = CurveUtil.transform(unsorted, xPos, 0.4f)

        assertEquals(expected, actual, EPSILON)
    }

    @Test
    fun `transform uses last y for duplicated x value`() {
        val data = listOf(0f to 0f, 1f to 10f, 1f to 15f, 2f to 20f)

        assertTransformY(15f, data, 1f, 0.4f)
        val interpolated = CurveUtil.transform(data, 1.5f, 0.4f)
        assertTrue(interpolated.isFinite(), "Expected finite interpolation result, got $interpolated")
    }

    @Test
    fun `transform normalizes out-of-range and non-finite tension`() {
        val data = listOf(0f to 0f, 1f to 5f, 2f to 20f)
        val xPos = 0.6f

        val tensionZero = CurveUtil.transform(data, xPos, 0f)
        assertEquals(tensionZero, CurveUtil.transform(data, xPos, -10f), EPSILON)
        assertEquals(CurveUtil.transform(data, xPos, 1f), CurveUtil.transform(data, xPos, 10f), EPSILON)
        assertEquals(tensionZero, CurveUtil.transform(data, xPos, Float.NaN), EPSILON)
        assertEquals(tensionZero, CurveUtil.transform(data, xPos, Float.POSITIVE_INFINITY), EPSILON)
    }

    /**
     * Replacing `Pair<Float, Float>` with `Vector2f` for better readability
     */
    private infix fun Float.to(another: Float) = Vector2f(this, another)

    private fun assertTransformY(
        expectedY: Float,
        data: List<Vector2f>,
        xPos: Float,
        tension: Float,
        onOutOfBounds: CurveUtil.OnOutOfBounds = CurveUtil.OnOutOfBounds.CLAMP
    ) {
        assertEquals(expectedY, CurveUtil.transform(data, xPos, tension, onOutOfBounds), EPSILON)
    }
}
