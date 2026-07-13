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

import net.ccbluex.fastutil.asIntList
import net.ccbluex.fastutil.intListOf
import net.ccbluex.fastutil.mapToIntArray
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Font

class MinecraftTextProcessorTest {

    @Test
    fun testProcessMapsFontStyles() {
        val text = Component.empty()
            .append("p".asPlainText())
            .append("b".asPlainText(Style.EMPTY.withBold(true)))
            .append("i".asPlainText(Style.EMPTY.withItalic(true)))
            .append("x".asPlainText(Style.EMPTY.withBold(true).withItalic(true)))

        val processed = MinecraftTextProcessor.process(text, Color4b(1, 2, 3, 4))

        assertEquals(
            intListOf(Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD or Font.ITALIC),
            processed.chars.mapToIntArray { it.font }.asIntList(),
        )
        assertEquals("pbix", processed.chars.joinToString("") { it.char.toString() })
    }

    @Test
    fun testProcessAppliesDefaultAndStyledColors() {
        val defaultColor = Color4b(10, 20, 30, 40)
        val text = Component.empty()
            .append("a".asPlainText())
            .append("b".asPlainText(Style.EMPTY.withColor(0x336699)))

        val processed = MinecraftTextProcessor.process(text, defaultColor)

        assertEquals(defaultColor, processed.chars[0].color)
        assertEquals(Color4b.fullAlpha(0x336699), processed.chars[1].color)
    }

    @Test
    fun testProcessTracksUnderlineAndStrikethroughRanges() {
        val text = Component.empty()
            .append("ab".asPlainText(Style.EMPTY.withUnderlined(true)))
            .append("cd".asPlainText(Style.EMPTY.withStrikethrough(true)))

        val processed = MinecraftTextProcessor.process(text, Color4b(255, 255, 255, 255))

        assertEquals(listOf(0, 2), processed.underlines.toIntArray().toList())
        assertEquals(listOf(2, 4), processed.strikeThroughs.toIntArray().toList())
    }

    @Test
    fun testProcessUsesObfuscationCharsetWhenRequested() {
        val text = "abcd".asPlainText(Style.EMPTY.withObfuscated(true))
        val processed = MinecraftTextProcessor.process(text, Color4b(255, 255, 255, 255))
        val randomChars = TextProcessor.RANDOM_CHARS.toSet()

        assertEquals(4, processed.chars.size)
        assertTrue(processed.chars.all { it.obfuscated })
        assertTrue(processed.chars.all { it.char in randomChars })
    }
}
