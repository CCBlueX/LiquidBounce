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

package net.ccbluex.liquidbounce.config.utils

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue

fun interface AutoCompletionProvider {

    /**
     * Gives an array with all possible completions for the [value].
     */
    fun possible(value: Value<*>): Iterable<String>

    companion object Default : AutoCompletionProvider {
        override fun possible(value: Value<*>): Iterable<String> = emptyList()

        @JvmStatic
        fun ofConst(strings: List<String>): AutoCompletionProvider {
            return AutoCompletionProvider { strings }
        }

        @JvmField
        val booleanCompleter = ofConst(listOf("true", "false"))

        @JvmField
        val rangedCompleter = AutoCompletionProvider { value ->
            val range = (value as RangedValue<*>).range
            listOf(range.start.toString(), range.endInclusive.toString())
        }

        @JvmField
        val modeGroupCompleter = AutoCompletionProvider { value ->
            (value as ModeValueGroup<*>).modes.mapToArray { it.tag }.asList()
        }

        @JvmField
        val choiceListCompleter = AutoCompletionProvider { value ->
            (value as ChoiceListValue<*>).choices.mapToArray { it.tag }.asList()
        }

        @JvmField
        val multiChoiceCompleter = AutoCompletionProvider { value ->
            (value as MultiChoiceListValue<*>).choices.mapToArray { it.tag }.asList()
        }
    }

}
