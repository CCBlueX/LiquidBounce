package net.ccbluex.liquidbounce.render.engine.font.processor

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.text.Style
import net.minecraft.text.Text
import java.awt.Font
import java.util.Optional
import kotlin.random.Random

object MinecraftTextProcessor : TextProcessor(Random(Random.nextLong())) {

    private class ProcessResult(
        override val chars: ArrayList<ProcessedTextCharacter>,
        override val underlines: ArrayList<IntRange>,
        override val strikeThroughs: ArrayList<IntRange>,
    ) : ProcessedText

    override fun process(
        text: Text,
        defaultColor: Color4b,
    ): ProcessedText {
        val result = ProcessResult(ArrayList(), ArrayList(), ArrayList())
        text.visit({ style, asString ->
            visit(style, asString, defaultColor, result)
        }, Style.EMPTY)

        return result
    }

    private fun visit(
        style: Style,
        textAsString: String,
        defaultColor: Color4b,
        result: ProcessResult,
    ): Optional<Nothing> {
        val font = when {
            style.isBold && style.isItalic -> Font.BOLD or Font.ITALIC
            style.isBold -> Font.BOLD
            style.isItalic -> Font.ITALIC
            else -> Font.PLAIN
        }
        val color = style.color?.let { Color4b(it.rgb) } ?: defaultColor
        val obfuscated = style.isObfuscated

        result.chars.ensureCapacity(textAsString.length)
        for (char in textAsString.toCharArray()) {
            val actualChar = if (obfuscated) generateObfuscatedChar() else char

            result.chars.add(ProcessedTextCharacter(actualChar, font, obfuscated, color))
        }

        val start = result.chars.size - textAsString.length
        val end = result.chars.size

        val textRange = start until end

        if (style.isUnderlined) {
            result.underlines.add(textRange)
        }

        if (style.isStrikethrough) {
            result.strikeThroughs.add(textRange)
        }

        return Optional.empty()
    }

}
