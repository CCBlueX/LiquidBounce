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

class JoinedListTest {

    @Test
    fun `iterates as join with separator`() {
        val joined = JoinedList(listOf("a", "b", "c"), "-")

        assertEquals(5, joined.size)
        assertEquals(listOf("a", "-", "b", "-", "c"), joined)
        assertEquals("a", joined[0])
        assertEquals("-", joined[1])
        assertEquals("b", joined[2])
        assertEquals("-", joined[3])
        assertEquals("c", joined[4])
    }

    @Test
    fun `empty source yields empty list`() {
        val joined = JoinedList(emptyList<Int>(), 0)

        assertEquals(0, joined.size)
        assertEquals(emptyList<Int>(), joined)
    }

    @Test
    fun `single element yields no separator`() {
        val joined = JoinedList(listOf("x"), "-")

        assertEquals(1, joined.size)
        assertEquals(listOf("x"), joined)
    }

    @Test
    fun `is a live view of source`() {
        val source = mutableListOf("a", "b")
        val joined = JoinedList(source, "-")

        source.add("c")
        source[0] = "x"

        assertEquals(5, joined.size)
        assertEquals(listOf("x", "-", "b", "-", "c"), joined)
    }

    @Test
    fun `is read only`() {
        val joined = JoinedList(listOf("a"), "-")

        assertFailsWith<UnsupportedOperationException> { joined.add("b") }
        assertFailsWith<UnsupportedOperationException> { joined.removeAt(0) }
    }

    @Test
    fun `throws on out of bounds access`() {
        val joined = JoinedList(listOf("a"), "-")

        assertFailsWith<IndexOutOfBoundsException> { joined[-1] }
        assertFailsWith<IndexOutOfBoundsException> { joined[1] }
    }

}
