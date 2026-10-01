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

package net.ccbluex.liquidbounce.utils.text

import net.minecraft.network.chat.Style
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppenderCharSinkTest {

    private val supplementaryCodepoint = 0x20000

    @Test
    fun `appends a supplementary codepoint as a single character`() {
        val builder = StringBuilder()
        val sink = AppenderCharSink(builder)

        assertTrue(sink.accept(0, Style.EMPTY, supplementaryCodepoint))
        assertContentEquals(intArrayOf(supplementaryCodepoint), builder.codePoints().toArray())
    }

    @Test
    fun `a cleared sink can be reused`() {
        val builder = StringBuilder()
        val sink = AppenderCharSink(builder)

        sink.accept(0, Style.EMPTY, 'a'.code)
        sink.clear()
        sink.accept(0, Style.EMPTY, 'b'.code)

        assertEquals("b", builder.toString())
    }

    @Test
    fun `collects the whole sequence and does not leak it into the next call`() {
        val first = AppenderCharSink.codePointsToString("Vanilla joined".withFormat())
        val second = AppenderCharSink.codePointsToString("a\uD83D\uDE00".withFormat())

        assertEquals("Vanilla joined", first)
        assertEquals("a\uD83D\uDE00", second)
    }

}
