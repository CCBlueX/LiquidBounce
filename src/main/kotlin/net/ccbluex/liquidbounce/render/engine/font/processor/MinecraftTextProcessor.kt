package net.ccbluex.liquidbounce.render.engine.font.processor

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.collection.Pool
import net.minecraft.text.Style
import net.minecraft.text.Text
import java.awt.Font
import java.util.ArrayDeque
import java.util.Optional
import kotlin.random.Random

object MinecraftTextProcessor : TextProcessor<MinecraftTextProcessor.RecyclingProcessedText>() {

    private val defaultRng = Random(Random.nextLong())

    val TEXT_POOL = Pool(ArrayDeque(), {
        RecyclingProcessedText(ArrayList(), ArrayList(), ArrayList())
    }) {
        it.chars.clear()
        it.underlines.clear()
        it.strikeThroughs.clear()
    }

    class RecyclingProcessedText(
        override var chars: ArrayList<ProcessedText.ProcessedChar>,
        override var underlines: ArrayList<IntRange>,
        override var strikeThroughs: ArrayList<IntRange>,
    ) : ProcessedText

    override fun process(
        text: Text,
        defaultColor: Color4b,
    ): RecyclingProcessedText {
        val result = TEXT_POOL.take()
        text.visit({ style, asString ->
            visit(style, asString, defaultColor, result)
        }, Style.EMPTY)

        return result
    }

    private fun visit(
        style: Style,
        textAsString: String,
        defaultColor: Color4b,
        result: RecyclingProcessedText,
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
        var rng: Random? = null
        for (char in textAsString) {
            val actualChar = if (obfuscated) {
                if (rng == null) rng = Random(defaultRng.nextLong())
                generateObfuscatedChar(rng)
            } else {
                char
            }

            result.chars.add(ProcessedText.ProcessedChar(actualChar, font, obfuscated, color))
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
