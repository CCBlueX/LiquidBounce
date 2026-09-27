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

import net.ccbluex.liquidbounce.render.engine.font.FontStyle
import net.ccbluex.liquidbounce.render.engine.type.Color4b

interface ProcessedText {
    val chars: List<ProcessedChar>

    @JvmRecord
    data class ProcessedChar(
        val codepoint: Int,
        val style: Int,
        val color: Color4b,
    ) {
        /**
         * The [java.awt.Font] value, which is all the glyph lookup accepts.
         */
        val font: @FontStyle Int get() = style and FONT_MASK

        val obfuscated: Boolean get() = style and OBFUSCATED != 0

        val underlined: Boolean get() = style and UNDERLINE != 0

        val strikethrough: Boolean get() = style and STRIKETHROUGH != 0
    }

    companion object {
        /**
         * Bits 0 and 1 hold the [java.awt.Font] value (plain, bold, italic or both), which the glyph
         * lookup uses as a style index.
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
