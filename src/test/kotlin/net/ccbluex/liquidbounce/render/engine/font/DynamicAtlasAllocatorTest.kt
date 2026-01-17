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

package net.ccbluex.liquidbounce.render.engine.font

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Dimension

class DynamicAtlasAllocatorTest {
    fun validateTree(allocator: DynamicAtlasAllocator, slice: AtlasSlice) {
        for (child in slice.children) {
            assertSame(slice, child.parent)

            validateTree(allocator, child)
        }

        if (slice.children.isNotEmpty()) {
            // Validate that all of the children combined are the same size as the parent

            assertEquals(slice.width * slice.height, slice.children.sumOf { it.width * it.height })
        }

        // Validate that none of the children intersect with each other
        for (i in slice.children.indices) {
            for (j in i + 1 until slice.children.size) {
                val a = slice.children[i]
                val b = slice.children[j]

                assert(a.x + a.width <= b.x || b.x + b.width <= a.x || a.y + a.height <= b.y || b.y + b.height <= a.y)
            }
        }

        // Validate that all of the children are within the parent
        for (child in slice.children) {
            assertTrue(child.x >= slice.x)
            assertTrue(child.y >= slice.y)
            assertTrue(child.x + child.width <= slice.x + slice.width)
            assertTrue(child.y + child.height <= slice.y + slice.height)
        }

        val isAllocated = allocator.availableSlices.contains(slice)

        slice.children.forEach { checkSliceAllocation(allocator, it, isAllocated) }
    }

    fun checkSliceAllocation(allocator: DynamicAtlasAllocator, slice: AtlasSlice, isParentAllocated: Boolean) {
        if (isParentAllocated) {
            assertFalse(allocator.availableSlices.contains(slice))
            assertFalse(slice.isAllocated)

            slice.children.forEach {
                checkSliceAllocation(allocator, it, true)
            }
        } else {
            val isAllocated = allocator.availableSlices.contains(slice)

            slice.children.forEach {
                checkSliceAllocation(allocator, it, isAllocated)
            }
        }
    }

    fun findHighestSlice(slice: AtlasSlice): AtlasSlice {
        val parent = slice.parent

        return if (parent == null) {
            slice
        } else {
            findHighestSlice(parent)
        }
    }

    @Test
    fun testAllocator() {
        val allocator = DynamicAtlasAllocator(Dimension(1024, 1024), 64, Dimension(8, 8))
        val availableSliceCount = allocator.availableSlices.size

        val dims = arrayOf(
            Dimension(32, 32),
            Dimension(32, 64),
            Dimension(64, 32),
            Dimension(63, 31),
            Dimension(31, 32),
        )

        val slices = dims.map {
            val slice = allocator.allocate(it)

            validateTree(allocator, findHighestSlice(slice!!.internalSlice))

            slice
        }


        slices.forEach {
            val parent = findHighestSlice(it.internalSlice)

            allocator.free(it)

            validateTree(allocator, parent)
        }

        assertEquals(availableSliceCount, allocator.availableSlices.size)
    }
}
