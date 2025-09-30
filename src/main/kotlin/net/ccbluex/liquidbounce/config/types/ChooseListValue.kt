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

import com.google.gson.Gson
import com.google.gson.JsonElement
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.script.ScriptApiRequired

class ChooseListValue<T : NamedChoice>(
    name: String,
    aliases: List<String> = emptyList(),
    defaultValue: T,
    @Exclude val choices: Set<T>
) : Value<T>(name, aliases, defaultValue, ValueType.CHOOSE) {

    init {
        require(defaultValue in choices) { "default value must be in [${choices}]" }
    }

    override fun deserializeFrom(gson: Gson, element: JsonElement) {
        val name = element.asString

        setByString(name)
    }

    override fun setByString(string: String) {
        val newValue = choices.firstOrNull { it.choiceName == string }

        if (newValue == null) {
            throw IllegalArgumentException(
                "ChooseListValue `${this.name}` has no option named $string" +
                    " (available options are ${this.choices.joinToString { it.choiceName }})"
            )
        }

        set(newValue)
    }

    @ScriptApiRequired
    fun getChoicesStrings(): Array<String> {
        return choices.mapToArray { it.choiceName }
    }

}

interface NamedChoice {
    val choiceName: String

    companion object {
        @JvmName("of")
        @JvmStatic
        fun String.asNamedChoice(): NamedChoice = object : NamedChoice {
            override val choiceName get() = this@asNamedChoice

            override fun equals(other: Any?): Boolean =
                when (other) {
                    is NamedChoice -> other.choiceName == this.choiceName
                    is CharSequence -> this.choiceName == other
                    is Enum<*> -> this.choiceName == other.name
                    else -> false
                }

            override fun hashCode(): Int = this.choiceName.hashCode()

            override fun toString(): String = this.choiceName
        }
    }
}
