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
package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.types.AutoCompletionProvider.CompletionHandler
import net.ccbluex.liquidbounce.utils.kotlin.mapArray

object AutoCompletionProvider {

    val defaultCompleter = CompletionHandler { emptyArray() }

    val booleanCompleter = CompletionHandler { arrayOf("true", "false") }

    val choiceCompleter = CompletionHandler { value ->
        (value as ChoiceConfigurable<*>).choices.mapArray { it.choiceName }
    }

    val chooseCompleter = CompletionHandler { value ->
        (value as ChooseListValue<*>).choices.mapArray { it.choiceName }
    }

    fun interface CompletionHandler {

        /**
         * Gives an array with all possible completions for the [value].
         */
        fun possible(value: Value<*>): Array<String>

    }

}
