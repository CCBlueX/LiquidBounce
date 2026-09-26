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
package net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NameProtectCodePointIndexTest {

    @Test
    fun `maps onto itself without surrogate pairs`() {
        val text = "Hello"

        for (charIndex in 0..text.length) {
            assertEquals(charIndex, text.codePointIndex(charIndex))
        }
    }

    @Test
    fun `skips a leading surrogate pair`() {
        val text = "\uD83D\uDE00ab"
        assertEquals(3, text.codePointCount(0, text.length))

        assertEquals(0, text.codePointIndex(0))
        assertEquals(1, text.codePointIndex(2))
        assertEquals(2, text.codePointIndex(3))
        assertEquals(3, text.codePointIndex(4))
    }

    @Test
    fun `skips a surrogate pair in the middle`() {
        val text = "a\uD83D\uDE00b"
        assertEquals(3, text.codePointCount(0, text.length))

        assertEquals(0, text.codePointIndex(0))
        assertEquals(1, text.codePointIndex(1))
        assertEquals(2, text.codePointIndex(3))
        assertEquals(3, text.codePointIndex(4))
    }

    @Test
    fun `skips multiple surrogate pairs`() {
        val text = "\uD83D\uDE00\uD83D\uDE00a"
        assertEquals(3, text.codePointCount(0, text.length))

        assertEquals(1, text.codePointIndex(2))
        assertEquals(2, text.codePointIndex(4))
        assertEquals(3, text.codePointIndex(5))
    }

    @Test
    fun `treats an unpaired surrogate as one char`() {
        val text = "\uD800a"

        assertEquals(0, text.codePointIndex(0))
        assertEquals(1, text.codePointIndex(1))
        assertEquals(2, text.codePointIndex(2))
    }

}
