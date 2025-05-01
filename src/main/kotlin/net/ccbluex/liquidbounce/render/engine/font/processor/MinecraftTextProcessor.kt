package net.ccbluex.liquidbounce.render.engine.font.processor

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.text.StringVisitable.StyledVisitor
import net.minecraft.text.Style
import net.minecraft.text.Text
import java.awt.Font
import java.util.*

class MinecraftTextProcessor(
    val text: Text,
    val defaultColor: Color4b,
    obfuscationSeed: Long?
) : TextProcessor(obfuscationSeed), StyledVisitor<Unit> {
    private val chars = ArrayList<ProcessedTextCharacter>()
    private val underlines = ArrayList<IntRange>()
    private val strikethroughs = ArrayList<IntRange>()

    init {
        text.visit(this, Style.EMPTY)
    }

    override fun process(): ProcessedText {
        return ProcessedText(chars, underlines, strikethroughs)
    }

    override fun accept(style: Style, text: String): Optional<Unit> {
        val font = when {
            style.isBold && style.isItalic -> Font.BOLD or Font.ITALIC
            style.isBold -> Font.BOLD
            style.isItalic -> Font.ITALIC
            else -> Font.PLAIN
        }
        val color = style.color?.rgb?.let { Color4b(it) } ?: defaultColor
        val obfuscated = style.isObfuscated

        for (char in text.toCharArray()) {
            val actualChar = if (obfuscated) generateObfuscatedChar() else char

            this.chars.add(ProcessedTextCharacter(actualChar, font, obfuscated, color))
        }

        val start = this.chars.size - text.length
        val end = this.chars.size

        val textRange = start until end

        if (style.isUnderlined) {
            this.underlines.add(textRange)
        }

        if (style.isStrikethrough) {
            this.strikethroughs.add(textRange)
        }

        return Optional.empty()
    }

}
