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
package net.ccbluex.liquidbounce.utils.kotlin

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.collection.LruCache
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class MutableCopyTest {

    private fun <T : Any> assertCopied(value: T, change: T.() -> Unit, read: T.() -> Any?) {
        val before = value.read()
        @Suppress("UNCHECKED_CAST")
        val copy = copyIfMutable(value) as T

        assertNotSame(value, copy)
        assertSame(value.javaClass, copy.javaClass)
        assertEquals(before, copy.read())

        copy.change()
        assertEquals(before, value.read(), "changing the copy reached the original")
    }

    @Test
    fun `collections and maps are copied as the same class`() {
        assertCopied(arrayListOf(1, 2), { add(3) }, { toList() })
        assertCopied(ArrayDeque(listOf(1, 2)), { addFirst(0) }, { toList() })
        assertCopied(ConcurrentHashMap(mapOf("a" to 1)), { put("b", 2) }, { toMap() })
        assertCopied(IntOpenHashSet(intArrayOf(1, 2)), { add(3) }, { toSet() })
        assertCopied(Object2IntOpenHashMap(mapOf("a" to 1)), { put("a", 2) }, { toMap() })
    }

    @Test
    fun `a copied cache keeps its limit`() {
        val cache = LruCache<Int, Int>(2).apply { put(1, 1); put(2, 2) }
        @Suppress("UNCHECKED_CAST")
        val copy = copyIfMutable(cache) as LruCache<Int, Int>

        copy[3] = 3
        assertEquals(setOf(2, 3), copy.keys)
        assertEquals(setOf(1, 2), cache.keys)
    }

    @Test
    fun `holders that are changed in place are copied`() {
        assertCopied(AtomicInteger(1), { set(2) }, { get() })
        assertCopied(Chronometer(1), { reset(2) }, { elapsedUntil(0) })
        assertCopied(intArrayOf(1, 2), { this[0] = 3 }, { toList() })
        assertCopied(arrayOf("a"), { this[0] = "b" }, { toList() })
    }

    @Test
    fun `immutable values are shared`() {
        val list = listOf(1, 2)
        val text = "text"

        assertSame(list, copyIfMutable(list))
        assertSame(text, copyIfMutable(text))
        assertSame(emptyList<Int>(), copyIfMutable(emptyList<Int>()))
    }

}
