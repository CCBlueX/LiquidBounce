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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
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

    @Test
    fun testInitSkipsRemainderSliceBelowMinHeight() {
        val allocator = DynamicAtlasAllocator(Dimension(32, 10), 6, Dimension(1, 5))

        assertEquals(1, allocator.availableSlices.size)
        val slice = allocator.availableSlices.single()
        assertEquals(0, slice.x)
        assertEquals(0, slice.y)
        assertEquals(32, slice.width)
        assertEquals(6, slice.height)
    }

    @Test
    fun testAllocateReturnsNullWhenNoSliceFits() {
        val allocator = DynamicAtlasAllocator(Dimension(16, 16), 16, Dimension(4, 4))

        assertNull(allocator.allocate(Dimension(17, 1)))
        assertNull(allocator.allocate(Dimension(1, 17)))
    }

    @Test
    fun testAllocateSplitsIntoFourSlices() {
        val allocator = DynamicAtlasAllocator(Dimension(20, 20), 20, Dimension(4, 4))

        val handle = allocator.allocate(Dimension(8, 8))
        assertNotNull(handle)

        assertEquals(8, handle!!.dimension.x)
        assertEquals(8, handle.dimension.y)
        assertEquals(3, allocator.availableSlices.size)
        assertTrue(allocator.availableSlices.any { it.x == 8 && it.y == 8 && it.width == 12 && it.height == 12 })
        assertTrue(allocator.availableSlices.any { it.x == 0 && it.y == 8 && it.width == 8 && it.height == 12 })
        assertTrue(allocator.availableSlices.any { it.x == 8 && it.y == 0 && it.width == 12 && it.height == 8 })
    }

    @Test
    fun testAllocateSplitsVerticallyWhenOnlyWidthRemainderFitsMinDimension() {
        val allocator = DynamicAtlasAllocator(Dimension(20, 10), 10, Dimension(4, 4))

        val handle = allocator.allocate(Dimension(8, 8))
        assertNotNull(handle)

        assertEquals(8, handle!!.dimension.x)
        assertEquals(10, handle.dimension.y)
        assertEquals(1, allocator.availableSlices.size)
        assertTrue(allocator.availableSlices.any { it.x == 8 && it.y == 0 && it.width == 12 && it.height == 10 })
    }

    @Test
    fun testAllocateSplitsHorizontallyWhenOnlyHeightRemainderFitsMinDimension() {
        val allocator = DynamicAtlasAllocator(Dimension(10, 20), 20, Dimension(4, 4))

        val handle = allocator.allocate(Dimension(8, 8))
        assertNotNull(handle)

        assertEquals(10, handle!!.dimension.x)
        assertEquals(8, handle.dimension.y)
        assertEquals(1, allocator.availableSlices.size)
        assertTrue(allocator.availableSlices.any { it.x == 0 && it.y == 8 && it.width == 10 && it.height == 12 })
    }

    @Test
    fun testAllocateDoesNotSplitWhenRemaindersAreTooSmall() {
        val allocator = DynamicAtlasAllocator(Dimension(10, 10), 10, Dimension(4, 4))

        val handle = allocator.allocate(Dimension(8, 8))
        assertNotNull(handle)
        assertEquals(10, handle!!.dimension.x)
        assertEquals(10, handle.dimension.y)
        assertTrue(allocator.availableSlices.isEmpty())
    }

    @Test
    fun testFreeCoalescesBackToHighestParent() {
        val allocator = DynamicAtlasAllocator(Dimension(16, 16), 16, Dimension(4, 4))

        val first = allocator.allocate(Dimension(8, 8))
        val second = allocator.allocate(Dimension(8, 8))

        assertNotNull(first)
        assertNotNull(second)

        allocator.free(first!!)
        allocator.free(second!!)

        assertEquals(1, allocator.availableSlices.size)
        val topSlice = allocator.availableSlices.single()
        assertEquals(0, topSlice.x)
        assertEquals(0, topSlice.y)
        assertEquals(16, topSlice.width)
        assertEquals(16, topSlice.height)
        assertTrue(topSlice.children.isEmpty())
        assertFalse(topSlice.isAllocated)
    }

    @Test
    fun testHandleCannotBeUsedAfterFree() {
        val allocator = DynamicAtlasAllocator(Dimension(16, 16), 16, Dimension(4, 4))

        val handle = allocator.allocate(Dimension(8, 8))
        assertNotNull(handle)

        allocator.free(handle!!)

        assertThrows(IllegalStateException::class.java) {
            handle.pos
        }
        assertThrows(IllegalStateException::class.java) {
            handle.dimension
        }
    }

    @Test
    fun testInitRejectsNonPositiveVerticalCutSize() {
        assertThrows(IllegalArgumentException::class.java) {
            DynamicAtlasAllocator(Dimension(16, 16), 0, Dimension(4, 4))
        }
        assertThrows(IllegalArgumentException::class.java) {
            DynamicAtlasAllocator(Dimension(16, 16), -1, Dimension(4, 4))
        }
    }

    @Test
    fun testFourSliceCutUsesMinWidthForHorizontalRemainder() {
        val allocator = DynamicAtlasAllocator(Dimension(20, 20), 20, Dimension(10, 2))

        val handle = allocator.allocate(Dimension(13, 13))
        assertNotNull(handle)

        // Horizontal remainder is 7 (< min width 10), so allocator must not use 4-way split.
        assertEquals(20, handle!!.dimension.x)
        assertEquals(13, handle.dimension.y)
        assertEquals(1, allocator.availableSlices.size)
        assertTrue(allocator.availableSlices.any { it.x == 0 && it.y == 13 && it.width == 20 && it.height == 7 })
    }

    @Test
    fun testDoubleFreeThrows() {
        val allocator = DynamicAtlasAllocator(Dimension(16, 16), 16, Dimension(4, 4))
        val handle = allocator.allocate(Dimension(8, 8))
        assertNotNull(handle)

        allocator.free(handle!!)

        assertThrows(IllegalStateException::class.java) {
            allocator.free(handle)
        }
    }
}
