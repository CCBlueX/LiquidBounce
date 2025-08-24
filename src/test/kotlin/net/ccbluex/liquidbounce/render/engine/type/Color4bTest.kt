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

package net.ccbluex.liquidbounce.render.engine.type

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class Color4bTest {

    @Test
    fun `fromHex with RGB format`() {
        val color = Color4b.fromHex("FF0080")
        assertEquals(255, color.r)
        assertEquals(0, color.g)
        assertEquals(128, color.b)
        assertEquals(255, color.a)
    }

    @Test
    fun `fromHex with RGB format including hash`() {
        val color = Color4b.fromHex("#FF0080")
        assertEquals(255, color.r)
        assertEquals(0, color.g)
        assertEquals(128, color.b)
        assertEquals(255, color.a)
    }

    @Test
    fun `fromHex with ARGB format`() {
        val color = Color4b.fromHex("80FF0080")
        assertEquals(128, color.a)
        assertEquals(255, color.r)
        assertEquals(0, color.g)
        assertEquals(128, color.b)
    }

    @Test
    fun `fromHex with ARGB format including hash`() {
        val color = Color4b.fromHex("#80FF0080")
        assertEquals(128, color.a)
        assertEquals(255, color.r)
        assertEquals(0, color.g)
        assertEquals(128, color.b)
    }

    @Test
    fun `fromHex with invalid format`() {
        assertThrows<IllegalArgumentException> {
            Color4b.fromHex("FF00")
        }
    }
}
