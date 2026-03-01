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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GuiOverlapRearrangerTest {

    private data class TestRearrangeable(override var bounds: BoundingBox2f) : GuiRearrangeable

    @Test
    fun `rearrange mutable list keeps non-overlap geometry`() {
        val elements = mutableListOf(
            TestRearrangeable(BoundingBox2f(60f, 60f, 90f, 90f)),
            TestRearrangeable(BoundingBox2f(-20f, -20f, 0f, 0f)),
            TestRearrangeable(BoundingBox2f(20f, 20f, 40f, 40f)),
        )
        val originalBounds = elements.map { it.bounds }.toSet()

        GuiOverlapRearranger().rearrange(elements)

        assertEquals(originalBounds, elements.map { it.bounds }.toSet())
        assertNoOverlaps(elements)
    }

    @Test
    fun `rearrange mutable list resolves simple overlap`() {
        val elements = mutableListOf(
            TestRearrangeable(BoundingBox2f(0f, 0f, 10f, 10f)),
            TestRearrangeable(BoundingBox2f(0f, 0f, 10f, 10f)),
        )

        GuiOverlapRearranger().rearrange(elements)

        assertNoOverlaps(elements)
    }

    @Test
    fun `rearrange mutable list resolves chain overlap`() {
        val elements = mutableListOf(
            TestRearrangeable(BoundingBox2f(0f, 0f, 10f, 10f)),
            TestRearrangeable(BoundingBox2f(8f, 0f, 18f, 10f)),
            TestRearrangeable(BoundingBox2f(16f, 0f, 26f, 10f)),
        )

        GuiOverlapRearranger(maxIter = 16).rearrange(elements)

        assertNoOverlaps(elements)
    }

    @Test
    fun `rearrange handles empty and single element`() {
        val rearranger = GuiOverlapRearranger()
        val empty = mutableListOf<GuiRearrangeable>()
        val single = mutableListOf<GuiRearrangeable>(TestRearrangeable(BoundingBox2f(0f, 0f, 10f, 10f)))
        val originalSingleBounds = single[0].bounds

        rearranger.rearrange(empty)
        rearranger.rearrange(single)

        assertTrue(empty.isEmpty())
        assertEquals(originalSingleBounds, single[0].bounds)
    }

    @Test
    fun `collection input mutates element bounds in place`() {
        val a = TestRearrangeable(BoundingBox2f(0f, 0f, 10f, 10f))
        val b = TestRearrangeable(BoundingBox2f(0f, 0f, 10f, 10f))
        val source = listOf<GuiRearrangeable>(a, b)
        val oldA = a.bounds
        val oldB = b.bounds

        GuiOverlapRearranger().rearrange(source)

        assertTrue(a.bounds != oldA || b.bounds != oldB)
        assertNoOverlaps(source)
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
