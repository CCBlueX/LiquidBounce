package net.ccbluex.liquidbounce.render.engine.font.processor

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.text.Text
import kotlin.random.Random

abstract class TextProcessor<T : ProcessedText> {

    /**
     * @param defaultColor The color all chars are drawn when no style is specified from Minecraft formatting
     */
    abstract fun process(
        text: Text,
        defaultColor: Color4b,
    ): T

    companion object {
        /**
         * @param obfuscationRng The random for the obfuscation.
         *      If null, obfuscated characters will be replaced with `_`
         */
        @JvmStatic
        protected fun generateObfuscatedChar(obfuscationRng: Random?): Char {
            return obfuscationRng?.let { RANDOM_CHARS.random(it) } ?: '_'
        }

        /**
         * Contains the chars for the `§k` formatting
         */
        @JvmField
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

}
