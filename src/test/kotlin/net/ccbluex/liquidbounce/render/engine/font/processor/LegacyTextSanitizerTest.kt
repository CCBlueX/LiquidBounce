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

package net.ccbluex.liquidbounce.render.engine.font.processor

import net.ccbluex.liquidbounce.utils.kotlin.optional
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Style
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LegacyTextSanitizerTest {

    @Test
    fun testPassesThroughPlainText() {
        val baseStyle = Style.EMPTY.withItalic(true)

        assertEquals(
            listOf("This is a Test!" to baseStyle),
            sanitize(baseStyle, "This is a Test!")
        )
    }

    @Test
    fun testAppliesFormattingCodesAndReset() {
        val baseStyle = Style.EMPTY.withItalic(true)

        val result = sanitize(baseStyle, "pre§cR§lB§rN")

        assertEquals(4, result.size)
        assertEquals("pre", result[0].first)
        assertEquals(baseStyle, result[0].second)

        assertEquals("R", result[1].first)
        assertEquals(baseStyle.withColor(ChatFormatting.RED), result[1].second)

        assertEquals("B", result[2].first)
        assertEquals(baseStyle.withColor(ChatFormatting.RED).withBold(true), result[2].second)

        assertEquals("N", result[3].first)
        assertEquals(Style.EMPTY, result[3].second)
    }

    @Test
    fun testUnknownFormattingCodeKeepsCurrentStyle() {
        val baseStyle = Style.EMPTY.withUnderlined(true)

        assertEquals(
            listOf("A" to baseStyle, "B" to baseStyle),
            sanitize(baseStyle, "A§xB")
        )
    }

    @Test
    fun testTrailingSectionSignIsPreservedAsText() {
        assertEquals(
            listOf("value§" to Style.EMPTY),
            sanitize(Style.EMPTY, "value§")
        )
    }

    @Test
    fun testOnlyFormattingCodesProducesNoSegments() {
        assertEquals(emptyList<Pair<String, Style>>(), sanitize(Style.EMPTY, "§a§l"))
    }

    private fun sanitize(style: Style, text: String): List<Pair<String, Style>> {
        val contents = ArrayList<Pair<String, Style>>()
        val sanitizer = LegacyTextSanitizer { style, string ->
            contents.add(string to style)
            optional()
        }

        sanitizer.accept(style, text)

        return contents
    }

}
