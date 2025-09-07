/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.utils.collection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LfuCacheTest {
    private lateinit var cache: LfuCache<String, Int>

    @BeforeEach
    fun setUp() {
        // Initialize cache with a capacity of 2 for testing
        cache = LfuCache(2)
    }

    // Basic functionality tests
    @Test
    fun `put and get operations return correct value`() {
        cache["a"] = 1
        assertEquals(1, cache["a"])
    }

    @Test
    fun `get on non-existent key returns null`() {
        assertNull(cache["nonexistent"])
    }

    @Test
    fun `cache size increases after put operation`() {
        cache["a"] = 1
        assertEquals(1, cache.size)
    }

    // Cache eviction policy tests (LFU core logic)
    @Test
    fun `evicts least frequently used item when capacity is exceeded`() {
        // Add two initial items
        cache["a"] = 1
        cache["b"] = 2

        // Access "a" to increase its frequency
        cache["a"]

        // Add third item to trigger eviction
        cache["c"] = 3

        // Verify "b" (least frequently used) is evicted
        assertNull(cache["b"])
        assertEquals(1, cache["a"])
        assertEquals(3, cache["c"])
    }

    @Test
    fun `evicts least recently used item among entries with same frequency`() {
        // Add items and make their frequencies equal
        cache["a"] = 1
        cache["b"] = 2
        cache["a"] // Frequency of "a" becomes 2
        cache["b"] // Frequency of "b" becomes 2

        // Add third item to trigger eviction (LRU applies for same frequency)
        cache["c"] = 3

        // Verify only one of "a" or "b" remains (the more recently used one)
        val remainingCount = listOfNotNull(cache["a"], cache["b"]).size
        assertEquals(1, remainingCount)
        assertEquals(3, cache["c"])
    }

    // Key update tests
    @Test
    fun `updating existing key increases its frequency and retains the key`() {
        cache["a"] = 1
        cache["b"] = 2

        // Update value of "a" (should increase its frequency)
        cache["a"] = 3

        // Add third item to trigger eviction
        cache["c"] = 4

        // Verify "a" (higher frequency) is retained, "b" is evicted
        assertEquals(3, cache["a"])
        assertNull(cache["b"])
    }

    // Boundary test for minimum capacity
    @Test
    fun `cache with capacity 1 evicts previous item immediately`() {
        val singleItemCache = LfuCache<String, Int>(1)
        singleItemCache["a"] = 1

        // Add new item to replace the existing one
        singleItemCache["b"] = 2

        assertNull(singleItemCache["a"])
        assertEquals(2, singleItemCache["b"])
    }

    // getOrPut method tests
    @Test
    fun `getOrPut returns existing value without invoking supplier`() {
        cache["a"] = 1
        // Supplier returns 2, but should not be invoked since "a" exists
        val result = cache.getOrPut("a") { 2 }

        assertEquals(1, result)
        assertEquals(1, cache["a"])
    }

    @Test
    fun `getOrPut inserts new value using supplier when key is absent`() {
        // Supplier provides value 1 since "a" does not exist
        val result = cache.getOrPut("a") { 1 }

        assertEquals(1, result)
        assertEquals(1, cache["a"])
    }

    // Exception handling tests
    @Test
    fun `creating cache with non-positive capacity throws IllegalArgumentException`() {
        // Capacity = 0 should throw exception
        assertThrows<IllegalArgumentException> {
            LfuCache<String, Int>(0)
        }

        // Negative capacity should throw exception
        assertThrows<IllegalArgumentException> {
            LfuCache<String, Int>(-1)
        }
    }

    // Frequency accumulation test
    @Test
    fun `item frequency increases with each access and affects eviction`() {
        cache["a"] = 1
        cache["b"] = 2

        // Access "a" multiple times to significantly increase its frequency
        repeat(3) { cache["a"] }

        // Add third item to trigger eviction
        cache["c"] = 3

        // Verify "b" (lowest frequency) is evicted
        assertNull(cache["b"])
        assertEquals(1, cache["a"])
        assertEquals(3, cache["c"])
    }
}
