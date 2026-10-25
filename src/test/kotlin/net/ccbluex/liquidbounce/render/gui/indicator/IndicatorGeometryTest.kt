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

package net.ccbluex.liquidbounce.render.gui.indicator

import org.joml.Vector2f
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndicatorGeometryTest {

    @Test
    fun `offset inside the border is left alone`() {
        val result = clamp(offsetX = 10f, offsetY = -20f)

        assertEquals(10f, result.x)
        assertEquals(-20f, result.y)
    }

    @Test
    fun `offset past one side lands on that side, keeping its direction`() {
        val result = clamp(offsetX = 490f, offsetY = 5f)

        assertEquals(LIMIT_X, result.x, TOLERANCE)
        // 490 -> 49 is a scale of 0.1, so y follows it.
        assertEquals(0.5f, result.y, TOLERANCE)
        assertTrue(abs(result.y) < LIMIT_Y, "y must not be pulled to the border as well")
    }

    @Test
    fun `offset past a corner lands on the border along its own direction`() {
        val result = clamp(offsetX = 1000f, offsetY = 1000f)

        assertTrue(onBorder(result), "$result is not on the border")
        // Scaling the whole offset keeps its direction, so a diagonal one stays diagonal.
        assertEquals(result.x, result.y, TOLERANCE)
        // The tighter of the two axes decides the scale, so y reaches its limit and x stops short of its own.
        assertEquals(LIMIT_Y, result.y, TOLERANCE)
        assertTrue(result.x < LIMIT_X, "x must not be pulled onto the border on its own")
    }

    @Test
    fun `offset on a single axis only moves on that axis`() {
        val result = clamp(offsetX = 0f, offsetY = 1000f)

        assertEquals(0f, result.x)
        assertEquals(LIMIT_Y, result.y, TOLERANCE)
    }

    @Test
    fun `zero offset stays at the center`() {
        val result = clamp(offsetX = 0f, offsetY = 0f)

        assertEquals(0f, result.x)
        assertEquals(0f, result.y)
    }

    @Test
    fun `a margin wider than the screen collapses to the center`() {
        val result = clamp(offsetX = 500f, offsetY = 500f, margin = 500f)

        assertEquals(0f, result.x)
        assertEquals(0f, result.y)
    }

    @Test
    fun `clamping depends on the direction only, not on the length`() {
        // Two offsets on the same ray, one just outside and one far outside, have to land on the same point.
        // This is what keeps a target far behind the camera from drifting towards the center.
        val near = clamp(offsetX = 100f, offsetY = 50f)
        val far = clamp(offsetX = 1000f, offsetY = 500f)

        assertEquals(near.x, far.x, TOLERANCE)
        assertEquals(near.y, far.y, TOLERANCE)
        assertTrue(onBorder(near), "$near is not on the border")
    }

    @Test
    fun `a direction shorter than the screen still reaches the border`() {
        val result = IndicatorGeometry.borderPoint(
            directionX = 1f,
            directionY = 0f,
            halfWidth = 50f,
            halfHeight = 25f,
            margin = MARGIN,
        )

        assertEquals(LIMIT_X, result.x, TOLERANCE)
        assertEquals(0f, result.y)
    }

    @Test
    fun `a diagonal direction shorter than the screen lands on the tighter side`() {
        val result = IndicatorGeometry.borderPoint(0.1f, 0.1f, 50f, 25f, MARGIN)

        assertTrue(onBorder(result), "$result is not on the border")
        assertEquals(LIMIT_Y, result.y, TOLERANCE)
    }

    @Test
    fun `a direction without a direction collapses to the center`() {
        val result = IndicatorGeometry.borderPoint(0f, 0f, 50f, 25f, MARGIN)

        assertEquals(0f, result.x)
        assertEquals(0f, result.y)
    }

    @Test
    fun `a span already inside needs no shift`() {
        assertEquals(0f, IndicatorGeometry.fitInside(10f, 40f, 100f))
    }

    @Test
    fun `a span past the start is moved in`() {
        assertEquals(10f, IndicatorGeometry.fitInside(-10f, 40f, 100f))
    }

    @Test
    fun `a span past the end is moved back`() {
        assertEquals(-20f, IndicatorGeometry.fitInside(60f, 120f, 100f))
    }

    @Test
    fun `a span wider than the extent keeps its start inside`() {
        // Moving it to the start overflows the end instead, which is the least bad option.
        assertEquals(10f, IndicatorGeometry.fitInside(-10f, 150f, 100f))
    }

    private fun onBorder(offset: Vector2f): Boolean =
        abs(abs(offset.x) - LIMIT_X) < TOLERANCE || abs(abs(offset.y) - LIMIT_Y) < TOLERANCE

    private fun clamp(
        offsetX: Float,
        offsetY: Float,
        margin: Float = MARGIN,
    ): Vector2f = IndicatorGeometry.clampToBorder(
        offsetX = offsetX,
        offsetY = offsetY,
        halfWidth = 50f,
        halfHeight = 25f,
        margin = margin,
    )

    private companion object {
        const val TOLERANCE = 1e-4f
        const val MARGIN = 1f
        const val LIMIT_X = 50f - MARGIN
        const val LIMIT_Y = 25f - MARGIN
    }

}
