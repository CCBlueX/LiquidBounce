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

import it.unimi.dsi.fastutil.longs.LongArrayList
import net.ccbluex.fastutil.Pool
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.network.chat.Style
import net.minecraft.util.ARGB
import net.minecraft.util.FormattedCharSink
import net.minecraft.util.FormattedCharSequence
import java.awt.Font

/**
 * Resolves the minecraft markup of a [FormattedCharSequence] into the characters [FontRenderer]
 * draws and measures: a [java.awt.Font] style, a color and the decoration flags read back while
 * drawing.
 *
 * Obfuscated (`§k`) characters are replaced by a random one here, so the shadow and the main pass
 * draw the same ones instead of shuffling per pass.
 */
internal object MinecraftTextProcessor {

    @JvmField
    val TEXT_POOL = Pool(
        initializer = { RecyclingProcessedText(LongArrayList()) }
    ) {
        it.chars.clear()
    }

    /**
     * Contains the chars for the `§k` formatting
     */
    @JvmField
    val RANDOM_CHARS = "1234567890abcdefghijklmnopqrstuvwxyz~!@#$%^&*()-=_+{}[]".toByteArray(Charsets.US_ASCII)

    /**
     * @param defaultColor The color all chars are drawn when no style is specified from Minecraft formatting
     */
    fun process(text: FormattedCharSequence, defaultColor: Color4b): RecyclingProcessedText =
        TEXT_POOL.borrow().also { borrowed ->
            borrowed.reset(defaultColor)
            text.accept(borrowed)
        }

    class RecyclingProcessedText(
        @JvmField val chars: LongArrayList,
    ) : FormattedCharSink {

        private var defaultColor = Color4b.WHITE.argb

        fun reset(defaultColor: Color4b) {
            chars.clear()
            this.defaultColor = defaultColor.argb
        }

        override fun accept(position: Int, style: Style, codepoint: Int): Boolean {
            val font = when {
                style.isBold && style.isItalic -> Font.BOLD or Font.ITALIC
                style.isBold -> Font.BOLD
                style.isItalic -> Font.ITALIC
                else -> Font.PLAIN
            }

            val flags = font or
                (if (style.isObfuscated) ProcessedChar.OBFUSCATED else 0) or
                (if (style.isUnderlined) ProcessedChar.UNDERLINE else 0) or
                (if (style.isStrikethrough) ProcessedChar.STRIKETHROUGH else 0)

            chars.add(
                ProcessedChar(
                    if (style.isObfuscated) RANDOM_CHARS.random().toInt() else codepoint,
                    flags,
                    style.color?.let { ARGB.opaque(it.value) } ?: defaultColor,
                ).bits
            )

            return true
        }

        inline fun forEach(block: (ProcessedChar) -> Unit) {
            for (i in chars.indices) {
                block(ProcessedChar(this.chars.getLong(i)))
            }
        }

        inline fun forEachIndexed(block: (index: Int, ProcessedChar) -> Unit) {
            for (i in chars.indices) {
                block(i, ProcessedChar(this.chars.getLong(i)))
            }
        }

    }

    /**
     * One character of a processed text, packed into a single `long` and carried in a [LongArrayList],
     * so that processing a text allocates nothing per character.
     */
    @JvmInline
    value class ProcessedChar(val bits: Long) {
        constructor(
            codepoint: Int, // 21 (24)
            style: Int, // 5 (8)
            color: Int, // 32
        ) : this((color.toLong() shl 32) or (style.toLong() shl 24) or (codepoint.toLong()))

        val codepoint: Int get() = bits.toInt() and 0xFFFFFF

        val style: Int get() = bits.toInt() shr 24 and 0xFF

        val color: Int get() = (bits ushr 32).toInt()

        /**
         * The [java.awt.Font] value, which is all the glyph lookup accepts.
         */
        val font: @FontStyle Int get() = style and FONT_MASK

        val obfuscated: Boolean get() = style and OBFUSCATED != 0

        val underlined: Boolean get() = style and UNDERLINE != 0

        val strikethrough: Boolean get() = style and STRIKETHROUGH != 0

        companion object {
            /**
             * Bits 0 and 1 hold the [java.awt.Font] value (plain, bold, italic or both), which the
             * glyph lookup uses as a style index.
             */
            const val FONT_MASK = 0b11

            /**
             * Decorations that follow the character instead of the text, so they are read back from it.
             */
            const val OBFUSCATED = 1 shl 2
            const val UNDERLINE = 1 shl 3
            const val STRIKETHROUGH = 1 shl 4
        }
    }

}
