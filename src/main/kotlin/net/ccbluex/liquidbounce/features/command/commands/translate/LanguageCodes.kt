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
 */
package net.ccbluex.liquidbounce.features.command.commands.translate

import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import java.util.Locale

/**
 * All language codes like `en`, `en-US`, `de`, `zh-CN`
 * and their display name depending on the user's locale.
 *
 * Sorted with [String.Companion.CASE_INSENSITIVE_ORDER].
 *
 * Not all are supported by the translator.
 */
internal val languageCodes: Map<String, Locale> = Locale.getAvailableLocales().filter {
    it.language.isNotEmpty()
}.associateByTo(sortedMapOf(String.CASE_INSENSITIVE_ORDER)) {
    when {
        it.country.isEmpty() -> it.language
        else -> "${it.language}-${it.country}"
    }
}

fun ParameterBuilder<*>.autocompleteWithLanguageCodes() = autocompletedWith { begin, _ ->
    languageCodes.keys.filter { it.startsWith(begin, ignoreCase = true) }
}
