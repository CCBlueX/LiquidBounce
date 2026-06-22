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

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.network.chat.Component
import kotlin.random.Random

abstract class TextProcessor<T : ProcessedText> {

    /**
     * @param defaultColor The color all chars are drawn when no style is specified from Minecraft formatting
     */
    abstract fun process(
        text: Component,
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
        val RANDOM_CHARS = "1234567890abcdefghijklmnopqrstuvwxyz~!@#$%^&*()-=_+{}[]".toCharArray()

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
