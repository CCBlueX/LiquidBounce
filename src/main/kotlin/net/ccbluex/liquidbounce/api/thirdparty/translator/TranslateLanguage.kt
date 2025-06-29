/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.api.thirdparty.translator

/**
 * Represents a language used for translation. The interface allows for flexibility in defining
 * translation languages, accommodating both specific language codes (e.g., "en-US", "ru-RU") and special
 * cases like "auto" for automatic language detection.
 *
 * This interface was created to handle situations where different APIs might have different
 * requirements:
 *
 * - Some translation APIs accept a literal language code (e.g., "en-US" for English, "ru-ru" for Russian).
 * - Other APIs, such as those that perform auto-detection, may use "auto" as a special keyword.
 * - The sealed interface structure ensures that only known and valid translation languages are used,
 *   while allowing easy extension in the future if new language types are required.
 *
 * By using an interface, we can define both special language cases (like `Auto`) and general language
 * codes (represented by `Literal`) in a type-safe and scalable manner.
 */
sealed interface TranslateLanguage {
    val literal: String

    object Auto : TranslateLanguage {
        override val literal = "auto"
    }

    class Literal internal constructor(override val literal: String) : TranslateLanguage

    companion object {
        @JvmStatic
        fun of(language: String): TranslateLanguage {
            return when (language.lowercase()) {
                "auto" -> Auto
                else -> Literal(language)
            }
        }
    }
}

fun String.asLanguage() = TranslateLanguage.of(this)
