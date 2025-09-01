package net.ccbluex.liquidbounce.render.engine.font.processor

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import kotlin.random.Random

/**
 * @param obfuscationSeed The seed for the obfuscation. If null, obfusscated characters will be replaced with `_`
 */
abstract class TextProcessor(obfuscationSeed: Long?) {
    private val obfuscationRng = obfuscationSeed?.let { Random(it) }

    abstract fun process(): ProcessedText

    protected fun generateObfuscatedChar(): Char {
        return obfuscationRng?.let { RANDOM_CHARS.random(it) } ?: '_'
    }

    companion object {
        /**
         * Contains the chars for the `§k` formatting
         */
        val RANDOM_CHARS = "1234567890abcdefghijklmnopqrstuvwxyz~!@#\$%^&*()-=_+{}[]".toCharArray()

        @JvmStatic
        val hexColors: Array<Color4b> = Array(16) { i ->
            val baseColor = (i shr 3 and 1) * 85
            val red = (i shr 2 and 1) * 170 + baseColor + if (i == 6) 85 else 0
            val green = (i shr 1 and 1) * 170 + baseColor
            val blue = (i and 1) * 170 + baseColor

            Color4b(red, green, blue, 255)
        }
    }
    data class ProcessedTextCharacter(val char: Char, val font: Int, val obfuscated: Boolean, val color: Color4b)
    data class ProcessedText(
        val chars: List<ProcessedTextCharacter>,
        val underlines: List<IntRange>,
        val strikeThroughs: List<IntRange>
    )
}
