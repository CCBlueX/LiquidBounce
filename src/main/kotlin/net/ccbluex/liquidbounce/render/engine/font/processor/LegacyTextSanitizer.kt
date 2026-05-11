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

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText.StyledContentConsumer
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.FormattedCharSink
import net.minecraft.util.StringDecomposer
import java.util.Optional

/**
 * This is a utility class which degenerates legacy formatting which is contained in new minecraft formatting
 * (i.e. `{text: "§a§lYeet"}`) into pure new formatting (i.e. `{text: "Yeet", bold: true, color: "green"}`).
 *
 * @see net.minecraft.util.StringDecomposer.iterateFormatted
 * @see net.minecraft.network.chat.Style.applyLegacyFormat
 *
 * @param innerVisitor the receiver of the degenerated text formatting.
 */
class LegacyTextSanitizer(
    private val innerVisitor: StyledContentConsumer<Nothing>
): StyledContentConsumer<Nothing> {

    override fun accept(style: Style, text: String): Optional<Nothing> {
        var currentStyle = style
        val currentText = StringBuilder(text.length)

        StringDecomposer.iterateFormatted(text, 0, style, style) { _, charStyle, codePoint ->
            if (charStyle != currentStyle) {
                flush(currentStyle, currentText)
                currentStyle = charStyle
            }

            currentText.appendCodePoint(codePoint)
            true
        }

        flush(currentStyle, currentText)
        return Optional.empty()
    }

    private fun flush(style: Style, text: StringBuilder) {
        if (text.isNotEmpty()) {
            this.innerVisitor.accept(style, text.toString())
            text.setLength(0)
        }
    }

    class SanitizedLegacyText(private val text: Component): FormattedCharSequence {
        override fun accept(visitor: FormattedCharSink): Boolean {
            return StringDecomposer.iterateFormatted(text, Style.EMPTY, visitor)
        }
    }
}
