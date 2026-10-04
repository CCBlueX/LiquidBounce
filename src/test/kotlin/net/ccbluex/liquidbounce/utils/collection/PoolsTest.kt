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
import kotlin.test.assertSame

class PoolsTest {

    private fun checkThrowingBuilder(capacity: Int?) {
        lateinit var builder: StringBuilder
        val failure = IllegalStateException("builder failed")
        val action: StringBuilder.() -> Unit = {
            builder = this
            append("unfinished")
            throw failure
        }

        val caught = assertFailsWith<IllegalStateException> {
            if (capacity == null) Pools.buildStringPooled(action) else Pools.buildStringPooled(capacity, action)
        }

        assertSame(failure, caught)
        assertEquals(0, builder.length, "Recycling must reset the builder even when the callback fails")
        assertEquals("next", Pools.buildStringPooled { append("next") })
    }

    @Test
    fun `recycles the builder when its callback throws`() = checkThrowingBuilder(null)

    @Test
    fun `recycles a capacity sized builder when its callback throws`() = checkThrowingBuilder(256)

    @Test
    fun `non local return recycles the builder`() {
        lateinit var builder: StringBuilder
        fun returnFromCallback(): String {
            Pools.buildStringPooled {
                builder = this
                append("unfinished")
                return "returned"
            }
            error("unreachable")
        }

        assertEquals("returned", returnFromCallback())
        assertEquals(0, builder.length)
    }

    @Test
    fun `non local return recycles the capacity sized builder`() {
        lateinit var builder: StringBuilder
        fun returnFromCallback(): String {
            Pools.buildStringPooled(256) {
                builder = this
                append("unfinished")
                return "returned"
            }
            error("unreachable")
        }

        assertEquals("returned", returnFromCallback())
        assertEquals(0, builder.length)
    }
}
