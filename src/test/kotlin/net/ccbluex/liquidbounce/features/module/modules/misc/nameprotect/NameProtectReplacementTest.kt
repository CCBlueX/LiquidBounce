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

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import net.ccbluex.fastutil.asIntList
import net.ccbluex.fastutil.intListOf
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import org.ahocorasick.trie.Emit
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NameProtectReplacementTest {

    private fun sequenceOf(value: String) = FormattedCharSequence.forward(value, Style.EMPTY)

    private fun replacement(start: Int, end: Int, name: String) = Emit(start, end, name) to
        NameProtectMappings.MappingData(name) { REPLACEMENT_COLOR }

    private fun render(sequence: FormattedCharSequence): Pair<String, IntList> {
        val text = StringBuilder()
        val indexes = IntArrayList()

        sequence.accept { index, _, codePoint ->
            indexes += index
            text.appendCodePoint(codePoint)
            true
        }

        return text.toString() to indexes
    }

    @Test
    fun `substitutes a match and shifts the text behind it`() {
        val rendered = render(sequenceOf("Vanilla joined").withReplacements(listOf(replacement(0, 6, "You"))))

        assertEquals("You joined" to IntArray(10) { it }.asIntList(), rendered)
    }

    @Test
    fun `substitutes a match at the start and at the end`() {
        val rendered = render(
            sequenceOf("Vanilla met Azuki").withReplacements(
                listOf(replacement(0, 6, "You"), replacement(12, 18, "her"))
            )
        )

        assertEquals("You met her" to (0 until 11).toList(), rendered)
    }

    @Test
    fun `counts a substitution holding a surrogate pair as two characters`() {
        val rendered = render(sequenceOf("a\uD83D\uDE00b").withReplacements(listOf(replacement(0, 0, "\uD835\uDD18"))))

        assertEquals("\uD835\uDD18\uD83D\uDE00b" to intListOf(0, 2, 4), rendered)
    }

    @Test
    fun `leaves text untouched when a match starts inside a surrogate pair`() {
        val rendered = render(sequenceOf("a\uD83D\uDE00b").withReplacements(listOf(replacement(2, 2, "x"))))

        assertEquals("a\uD83D\uDE00b" to intListOf(0, 1, 3), rendered)
    }

    @Test
    fun `colors the substitution and keeps the style of the rest`() {
        val styles = mutableListOf<Style>()

        sequenceOf("Vanilla!").withReplacements(listOf(replacement(0, 6, "You"))).accept { _, style, _ ->
            styles += style
            true
        }

        val replaced = Style.EMPTY.withColor(REPLACEMENT_COLOR.argb)

        assertEquals(listOf(replaced, replaced, replaced, Style.EMPTY), styles)
    }

    private companion object {
        private val REPLACEMENT_COLOR = Color4b.LIQUID_BOUNCE
    }

}
