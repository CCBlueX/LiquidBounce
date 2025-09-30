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

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlatMapVirtualTest {

    @Test
    fun `flatMapVirtual basic functionality`() {
        val original = listOf(1, 2, 3)
        val result = original.flatMapVirtual { listOf(it, it * 10) }

        assertEquals(6, result.size)
        assertEquals(listOf(1, 10, 2, 20, 3, 30), result)
    }

    @Test
    fun `flatMapVirtual with empty input list`() {
        val original = emptyList<Int>()
        val result = original.flatMapVirtual { listOf(it.toString()) }

        assertTrue(result.isEmpty())
        assertEquals(0, result.size)
    }

    @Test
    fun `flatMapVirtual with empty transformed lists`() {
        val original = listOf(1, 2, 3)
        val result = original.flatMapVirtual { if (it % 2 == 0) listOf(it) else emptyList() }

        assertEquals(1, result.size)
        assertEquals(listOf(2), result)
    }

    @Test
    fun `flatMapVirtual with single element`() {
        val original = listOf("test")
        val result = original.flatMapVirtual { it.toList() }

        assertEquals(4, result.size)
        assertEquals(listOf('t', 'e', 's', 't'), result)
    }

    @Test
    fun `flatMapVirtual with index out of bounds`() {
        val original = listOf(1, 2, 3)
        val result = original.flatMapVirtual { listOf(it) }

        assertThrows<IndexOutOfBoundsException> { result[-1] }
        assertThrows<IndexOutOfBoundsException> { result[3] }
        assertThrows<IndexOutOfBoundsException> { result[10] }
    }

    @Test
    fun `flatMapVirtual with large lists`() {
        val original = (1..1000).toList()
        val result = original.flatMapVirtual { (1..it).toList() }

        val expectedSize = original.sumOf { it }
        assertEquals(expectedSize, result.size)
        assertEquals(1, result[0])
        assertEquals(1000, result[expectedSize - 1])
    }

    @Test
    fun `flatMapVirtual matches standard flatMap`() {
        val original = listOf("apple", "banana", "cherry")
        val virtualResult = original.flatMapVirtual { it.uppercase().toList() }
        val standardResult = original.flatMap { it.uppercase().toList() }

        assertEquals(standardResult, virtualResult)
        assertEquals(standardResult.size, virtualResult.size)

        repeat(10) {
            val index = (0 until standardResult.size).random()
            assertEquals(standardResult[index], virtualResult[index])
        }
    }
}
