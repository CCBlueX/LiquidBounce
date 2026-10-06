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

package net.ccbluex.liquidbounce.utils.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LruCacheTest {

    @Test
    fun `retains exactly the configured number of entries`() {
        val cache = LruCache<Int, Int>(64)
        repeat(64) { cache[it] = it }

        assertEquals(64, cache.size)
        assertEquals((0 until 64).toList(), cache.keys.toList())

        cache[64] = 64

        assertEquals(64, cache.size)
        assertEquals((1..64).toList(), cache.keys.toList())
    }

    @Test
    fun `capacity one retains the latest entry`() {
        val cache = LruCache<String, Int>(1)
        cache["first"] = 1
        assertEquals(mapOf("first" to 1), cache)

        cache["second"] = 2
        assertEquals(mapOf("second" to 2), cache)
    }

    @Test
    fun `reading an entry protects it from the next eviction`() {
        val cache = LruCache<String, Int>(2)
        cache["first"] = 1
        cache["second"] = 2

        assertEquals(1, cache["first"])
        cache["third"] = 3

        assertEquals(listOf("first", "third"), cache.keys.toList())
    }

    @Test
    fun `replacing an entry updates recency without evicting another entry`() {
        val cache = LruCache<String, Int>(2)
        cache["first"] = 1
        cache["second"] = 2
        cache["first"] = 10

        assertEquals(2, cache.size)
        assertEquals(listOf("second", "first"), cache.keys.toList())
        assertEquals(10, cache["first"])

        cache["third"] = 3
        assertEquals(mapOf("first" to 10, "third" to 3), cache)
    }

    @Test
    fun `bulk insertion evicts the least recently used entries`() {
        val cache = LruCache<Int, Int>(2)
        cache.putAll(linkedMapOf(1 to 1, 2 to 2, 3 to 3, 4 to 4))

        assertEquals(listOf(3, 4), cache.keys.toList())
    }

    @Test
    fun `zero capacity does not retain entries`() {
        val cache = LruCache<String, Int>(0)
        cache["first"] = 1
        cache.putAll(mapOf("second" to 2, "third" to 3))

        assertTrue(cache.isEmpty())
    }

    @Test
    fun `negative capacity is rejected`() {
        assertFailsWith<IllegalArgumentException> { LruCache<String, Int>(-1) }
    }
}
