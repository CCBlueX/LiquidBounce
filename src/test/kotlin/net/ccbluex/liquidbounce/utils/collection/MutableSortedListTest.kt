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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse


class MutableSortedListTest {

    @Test
    fun `add maintains sorted order`() {
        val list = MutableSortedList<Int, Int> { it }
        list.add(5)
        list.add(1)
        list.add(3)

        assertEquals(listOf(1, 3, 5), list)
    }

    @Test
    fun `addAll maintains sorted order`() {
        val list = MutableSortedList<Int, Int> { it }
        list.addAll(listOf(5, 2, 4, 1))
        assertEquals(listOf(1, 2, 4, 5), list)
    }

    @Test
    fun `respects lower and upper bounds`() {
        val list = MutableSortedList<Int, Int>(
            lowerBound = 10,
            upperBound = 20
        ) { it }

        list.add(10)
        list.add(15)
        list.add(20)
        assertEquals(listOf(10, 15), list)

        assertFalse { list.add(5) }
        assertFalse { list.add(25) }
    }

    @Test
    fun `add(index) and set(index) should throw`() {
        val list = MutableSortedList<Int, Int> { it }
        list.add(1)
        list.add(2)

        assertThrows<UnsupportedOperationException> {
            list.add(0, 5)
        }

        assertThrows<UnsupportedOperationException> {
            list[0] = 10
        }
    }

    @Test
    fun `remove and clear should work normally`() {
        val list = MutableSortedList<Int, Int> { it }
        list.addAll(listOf(1, 2, 3, 4))
        assertTrue(list.remove(2))
        assertEquals(listOf(1, 3, 4), list)

        list.clear()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `binary insertion position is correct`() {
        val list = MutableSortedList<Int, Int> { it }
        list.addAll(listOf(10, 20, 30))
        list.add(25)
        assertEquals(listOf(10, 20, 25, 30), list)
    }
}
