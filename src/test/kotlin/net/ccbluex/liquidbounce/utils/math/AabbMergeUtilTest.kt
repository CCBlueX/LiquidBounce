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

import net.minecraft.world.phys.AABB
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AabbMergeUtilTest {

    @Test
    fun `returns empty list for empty input`() {
        assertTrue(mergeIntersectingAabbsSweep<Int>(emptyList()).isEmpty())
    }

    @Test
    fun `merges overlapping boxes with same key`() {
        val merged = mergeIntersectingAabbsSweep(
            listOf(
                KeyedAabb(box(0.0, 0.0, 0.0, 2.0, 2.0, 2.0), 1),
                KeyedAabb(box(1.0, 0.5, 0.5, 3.0, 2.5, 2.5), 1),
            )
        )

        assertEquals(1, merged.size)
        assertBoxEquals(box(0.0, 0.0, 0.0, 3.0, 2.5, 2.5), merged.single().box)
        assertEquals(1, merged.single().key)
    }

    @Test
    fun `does not merge overlapping boxes with different keys`() {
        val merged = mergeIntersectingAabbsSweep(
            listOf(
                KeyedAabb(box(0.0, 0.0, 0.0, 2.0, 2.0, 2.0), 1),
                KeyedAabb(box(1.0, 0.5, 0.5, 3.0, 2.5, 2.5), 2),
            )
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun `merges transitively connected overlaps`() {
        val merged = mergeIntersectingAabbsSweep(
            listOf(
                KeyedAabb(box(0.0, 0.0, 0.0, 2.0, 2.0, 2.0), 1),
                KeyedAabb(box(1.5, 0.0, 0.0, 3.5, 2.0, 2.0), 1),
                KeyedAabb(box(3.0, 0.0, 0.0, 5.0, 2.0, 2.0), 1),
            )
        )

        assertEquals(1, merged.size)
        assertBoxEquals(box(0.0, 0.0, 0.0, 5.0, 2.0, 2.0), merged.single().box)
    }

    @Test
    fun `does not merge boxes that only touch`() {
        val merged = mergeIntersectingAabbsSweep(
            listOf(
                KeyedAabb(box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), 1),
                KeyedAabb(box(1.0, 0.0, 0.0, 2.0, 1.0, 1.0), 1),
            )
        )

        assertEquals(2, merged.size)
    }

    private fun box(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): AABB =
        AABB(minX, minY, minZ, maxX, maxY, maxZ)

    private fun assertBoxEquals(expected: AABB, actual: AABB) {
        assertEquals(expected.minX, actual.minX, 1e-9)
        assertEquals(expected.minY, actual.minY, 1e-9)
        assertEquals(expected.minZ, actual.minZ, 1e-9)
        assertEquals(expected.maxX, actual.maxX, 1e-9)
        assertEquals(expected.maxY, actual.maxY, 1e-9)
        assertEquals(expected.maxZ, actual.maxZ, 1e-9)
    }
}
