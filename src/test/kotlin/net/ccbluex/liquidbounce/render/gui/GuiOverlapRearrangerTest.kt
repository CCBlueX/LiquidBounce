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

package net.ccbluex.liquidbounce.render.gui

import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuiOverlapRearrangerTest {

    private data class TestRearrangeable(override var bounds: BoundingBox2f) : GuiRearrangeable

    @Test
    fun `elements that do not overlap keep their bounds`() {
        val elements = listOf(
            TestRearrangeable(BoundingBox2f(60f, 60f, 90f, 90f)),
            TestRearrangeable(BoundingBox2f(-20f, -20f, 0f, 0f)),
            TestRearrangeable(BoundingBox2f(20f, 20f, 40f, 40f)),
        )
        val originalBounds = elements.map { it.bounds }

        GuiOverlapRearranger.rearrange(elements)

        assertEquals(originalBounds, elements.map { it.bounds })
    }

    @Test
    fun `a chain of overlaps is resolved`() {
        val elements = listOf(
            TestRearrangeable(BoundingBox2f(0f, 0f, 10f, 10f)),
            TestRearrangeable(BoundingBox2f(8f, 0f, 18f, 10f)),
            TestRearrangeable(BoundingBox2f(16f, 0f, 26f, 10f)),
        )

        GuiOverlapRearranger.rearrange(elements)

        assertNoOverlaps(elements)
    }

    @Test
    fun `elements keep the order they are given, even when they almost coincide`() {
        // These used to swap places on the slightest camera movement, because the layout order was derived from the
        // positions and a one pixel difference flipped it.
        val first = TestRearrangeable(BoundingBox2f(0f, 100f, 50f, 110f))
        val second = TestRearrangeable(BoundingBox2f(0f, 101f, 50f, 111f))

        GuiOverlapRearranger.rearrange(listOf(first, second))

        assertEquals(100f, first.bounds.yMin, "the first element keeps its place")
        assertEquals(110f, second.bounds.yMin, "the second one is pushed below it")
    }

    @Test
    fun `a pile at the same spot separates completely`() {
        val elements = List(12) { TestRearrangeable(BoundingBox2f(10f, 10f, 60f, 20f)) }

        GuiOverlapRearranger.rearrange(elements)

        assertNoOverlaps(elements)
    }

    @Test
    fun `elements only move vertically`() {
        val elements = listOf(
            TestRearrangeable(BoundingBox2f(0f, 0f, 10f, 10f)),
            TestRearrangeable(BoundingBox2f(2f, 2f, 12f, 12f)),
        )

        GuiOverlapRearranger.rearrange(elements)

        assertEquals(0f, elements[0].bounds.xMin)
        assertEquals(2f, elements[1].bounds.xMin, "the column is kept")
        assertNoOverlaps(elements)
    }

    @Test
    fun `rearrange handles empty and single element`() {
        val empty = listOf<GuiRearrangeable>()
        val single = listOf<GuiRearrangeable>(TestRearrangeable(BoundingBox2f(0f, 0f, 10f, 10f)))
        val originalSingleBounds = single[0].bounds

        GuiOverlapRearranger.rearrange(empty)
        GuiOverlapRearranger.rearrange(single)

        assertTrue(empty.isEmpty())
        assertEquals(originalSingleBounds, single[0].bounds)
    }

    @Test
    fun `bounds are mutated in place`() {
        val a = TestRearrangeable(BoundingBox2f(0f, 0f, 10f, 10f))
        val b = TestRearrangeable(BoundingBox2f(0f, 0f, 10f, 10f))
        val oldA = a.bounds
        val oldB = b.bounds

        GuiOverlapRearranger.rearrange(listOf(a, b))

        assertTrue(a.bounds != oldA || b.bounds != oldB)
        assertNoOverlaps(listOf(a, b))
    }

    private fun assertNoOverlaps(elements: List<GuiRearrangeable>) {
        for (i in elements.indices) {
            for (j in i + 1 until elements.size) {
                val a = elements[i].bounds
                val b = elements[j].bounds
                assertFalse(a intersects b, "Expected no overlap between $a and $b")
            }
        }
    }

}
