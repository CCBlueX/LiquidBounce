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

package net.ccbluex.liquidbounce.config.types.list

import com.google.gson.Gson
import com.google.gson.JsonElement
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.list.Tagged.Companion.makeLookupTable
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import java.util.SortedMap

class ChoiceListValue<T : Tagged>(
    name: String,
    aliases: List<String> = emptyList(),
    defaultValue: T,
    @Exclude val choices: Set<T>
) : Value<T>(name, aliases, defaultValue, ValueType.CHOOSE) {

    init {
        require(defaultValue in choices) { "default value must be in [${choices}]" }
    }

    @Exclude @ProtocolExclude
    private val choiceByName = choices.makeLookupTable()

    override fun deserializeFrom(gson: Gson, element: JsonElement) {
        val name = element.asString

        setByString(name)
    }

    override fun setByString(string: String) {
        val newValue = choiceByName[string] ?: throw IllegalArgumentException(
            "ChoiceListValue `${this.name}` has no option named $string" +
                " (available options are ${this.choices.joinToString { it.tag }})"
        )

        set(newValue)
    }

    @ScriptApiRequired
    fun getChoicesStrings(): Array<String> {
        return choices.mapToArray { it.tag }
    }

}

interface Tagged {
    val tag: String

    val tagAliases: List<String> get() = emptyList()

    companion object {
        @JvmStatic
        fun <T : Tagged> Iterable<T>.makeLookupTable(): SortedMap<String, T> {
            val map = Object2ObjectRBTreeMap<String, T>(String.CASE_INSENSITIVE_ORDER)
            for (item in this) {
                if (map.put(item.tag, item) != null) {
                    throw IllegalArgumentException("Duplicate tag: ${item.tag}")
                }
                for (alias in item.tagAliases) {
                    if (map.put(alias, item) != null) {
                        throw IllegalArgumentException("Duplicate alias: $alias")
                    }
                }
            }
            return map
        }

        @JvmName("of")
        @JvmStatic
        fun String.asTagged(): Tagged = object : Tagged, Comparable<Tagged> {
            override val tag get() = this@asTagged

            override fun equals(other: Any?): Boolean =
                when (other) {
                    is Tagged -> other.tag == this.tag
                    is CharSequence -> this.tag == other
                    is Enum<*> -> this.tag == other.name
                    else -> false
                }

            override fun hashCode(): Int = this.tag.hashCode()

            override fun toString(): String = this.tag

            override fun compareTo(other: Tagged): Int = this.tag.compareTo(other.tag)
        }
    }
}
