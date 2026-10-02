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

package net.ccbluex.liquidbounce.render.engine.font

import net.ccbluex.fastutil.mapToIntArray
import net.ccbluex.liquidbounce.render.engine.font.MinecraftTextProcessor.ProcessedChar
import net.ccbluex.liquidbounce.render.engine.font.MinecraftTextProcessor.RecyclingProcessedText
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.text.asFormattedCharSequence
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.awt.Font

class MinecraftTextProcessorTest {

    private fun process(text: Component, defaultColor: Color4b) =
        MinecraftTextProcessor.process(text.asFormattedCharSequence(), defaultColor)

    private fun RecyclingProcessedText.pack(): List<ProcessedChar> {
        val chars = ArrayList<ProcessedChar>(this.chars.size)
        forEach { chars += it }

        return chars
    }

    @Test
    fun testProcessMapsFontStyles() {
        val text = Component.empty()
            .append("p".asPlainText())
            .append("b".asPlainText(Style.EMPTY.withBold(true)))
            .append("i".asPlainText(Style.EMPTY.withItalic(true)))
            .append("x".asPlainText(Style.EMPTY.withBold(true).withItalic(true)))

        val chars = process(text, Color4b(1, 2, 3, 4)).pack()

        assertContentEquals(
            intArrayOf(Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD or Font.ITALIC),
            chars.mapToIntArray { it.font },
        )
        assertEquals("pbix", chars.joinToString("") { Character.toString(it.codepoint) })
    }

    @Test
    fun testProcessAppliesDefaultAndStyledColors() {
        val defaultColor = Color4b(10, 20, 30, 40)
        val text = Component.empty()
            .append("a".asPlainText())
            .append("b".asPlainText(Style.EMPTY.withColor(0x336699)))

        val chars = process(text, defaultColor).pack()

        assertEquals(defaultColor.argb, chars[0].color)
        assertEquals(Color4b.fullAlpha(0x336699).argb, chars[1].color)
    }

    /**
     * Legacy formatting codes are what [net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput]
     * degenerates, so a `§`-colored string has to end up styled like its styled counterpart.
     */
    @Test
    fun testProcessDegeneratesLegacyFormatting() {
        val legacy = process("§cred".asPlainText(), Color4b.WHITE).pack().map { it.color }
        val styled = process(
            "red".asPlainText(Style.EMPTY.applyFormat(ChatFormatting.RED)),
            Color4b.WHITE,
        ).pack().map { it.color }

        assertContentEquals(styled, legacy)
    }

    @Test
    fun testProcessTracksUnderlineAndStrikethrough() {
        val text = Component.empty()
            .append("ab".asPlainText(Style.EMPTY.withUnderlined(true)))
            .append("cd".asPlainText(Style.EMPTY.withStrikethrough(true)))

        val chars = process(text, Color4b.WHITE).pack()

        assertContentEquals(listOf(true, true, false, false), chars.map { it.underlined })
        assertContentEquals(listOf(false, false, true, true), chars.map { it.strikethrough })
    }

    @Test
    fun testProcessUsesObfuscationCharsetWhenRequested() {
        val text = "abcd".asPlainText(Style.EMPTY.withObfuscated(true))
        val chars = process(text, Color4b.WHITE).pack()

        assertEquals(4, chars.size)
        assertTrue(chars.all { it.obfuscated })
        assertTrue(chars.all { it.codepoint.toByte() in MinecraftTextProcessor.RANDOM_CHARS })
    }

    @Test
    fun testProcessKeepsSupplementaryCodepointsIntact() {
        val supplementaryCodepoint = 0x20000
        val text = "a${Character.toString(supplementaryCodepoint)}b"
            .asPlainText(Style.EMPTY.withUnderlined(true))

        val chars = process(text, Color4b.WHITE).pack()

        assertContentEquals(
            intArrayOf('a'.code, supplementaryCodepoint, 'b'.code),
            chars.mapToIntArray { it.codepoint }
        )
        assertTrue(chars.all { it.underlined })
    }
}
