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

import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.MutableText

sealed class TranslationResult(
    val isValid: Boolean
) {
    abstract fun toResultText(): MutableText

    data class Success(
        val origin: String,
        val translation: String,
        val fromLanguage: TranslateLanguage,
        val toLanguage: TranslateLanguage
    ) : TranslationResult(
        origin != translation && fromLanguage != toLanguage
    ) {
        override fun toResultText(): MutableText = "".asText()
            .append(regular("("))
            .append(variable(fromLanguage.literal))
            .append(regular("->"))
            .append(variable(toLanguage.literal))
            .append(regular(") "))
            .append(regular(translation).copyable(copyContent = translation))
    }

    data class Failure(
        val ex: Exception,
    ) : TranslationResult(false) {
        override fun toResultText(): MutableText = "".asText()
            .append(markAsError("Failed to translate (${ex.javaClass.simpleName}): ${ex.message}"))
    }
}
