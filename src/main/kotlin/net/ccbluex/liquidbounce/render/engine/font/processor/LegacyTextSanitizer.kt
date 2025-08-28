package net.ccbluex.liquidbounce.render.engine.font.processor

import net.minecraft.text.CharacterVisitor
import net.minecraft.text.OrderedText
import net.minecraft.text.StringVisitable.StyledVisitor
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*

/**
 * This is a utility class which degenerates legacy formatting which is contained in new minecraft formatting
 * (i.e. `{text: "§a§lYeet"}`) into pure new formatting (i.e. `{text: "Yeet", bold: true, color: "green"}`).
 *
 * @param innerVisitor the receiver of the degenerated text formatting.
 */
class LegacyTextSanitizer(
    private val innerVisitor: StyledVisitor<Nothing>
): StyledVisitor<Nothing> {

    override fun accept(style: Style, text: String): Optional<Nothing> {
        var currentStyle = style

        var currentIndex = 0

        while (currentIndex < text.length) {
            val nextCommand = text.indexOf('§', currentIndex)

            // If there is no more paragraph or if the paragraph is the last in the text, stop the processing.
            if (nextCommand == -1 || nextCommand + 1 >= text.length) {
                break
            }


            // If there is text before the paragraph, accept it first
            if (currentIndex != nextCommand) {
                this.innerVisitor.accept(currentStyle, text.substring(currentIndex, nextCommand))
            }

            val nextCode = text.codePointAt(nextCommand + 1)

            currentStyle = applyCodeForStyle(nextCode, currentStyle)
            // skip the §X characters
            currentIndex = nextCommand + 2
        }

        if (currentIndex != text.length) {
            this.innerVisitor.accept(currentStyle, text.substring(currentIndex))
        }

        return Optional.empty()
    }

    private fun applyCodeForStyle(codePoint: Int, currentStyle: Style): Style {
        return Formatting.byCode(codePoint.toChar())?.applyFormatting(currentStyle) ?: currentStyle
    }

    private fun Formatting.applyFormatting(style: Style): Style {
        return when {
            isColor -> style.withColor(this)
            else -> when (this) {
                Formatting.RESET -> Style.EMPTY
                Formatting.BOLD -> style.withBold(true)
                Formatting.OBFUSCATED -> style.withObfuscated(true)
                Formatting.STRIKETHROUGH -> style.withStrikethrough(true)
                Formatting.UNDERLINE -> style.withUnderline(true)
                Formatting.ITALIC -> style.withItalic(true)
                else -> style
            }
        }
    }

    class SanitizedLegacyText(private val text: Text): OrderedText {
        override fun accept(visitor: CharacterVisitor): Boolean {
            val degenerator = LegacyTextSanitizer { style, text ->
                var idx = 0

                text.chars().forEach { codePoint ->
                    visitor.accept(idx, style, codePoint)
                    idx++
                }

                Optional.empty()
            }

            text.visit(degenerator, Style.EMPTY)

            return true
        }
    }
}
