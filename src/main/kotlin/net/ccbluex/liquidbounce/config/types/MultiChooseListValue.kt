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
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import java.util.TreeMap

class MultiChooseListValue<T : NamedChoice>(
    name: String,
    /**
     * Enabled values. A mutable and unordered [Set].
     */
    value: MutableSet<T>,
    /**
     * All selectable choices. A readonly and ordered [Set].
     */
    @Exclude val choices: Set<T>,

    /**
     * Can deselect all values or enable at least one
     */
    @Exclude val canBeNone: Boolean = true,
) : Value<MutableSet<T>>(
    name,
    defaultValue = value,
    valueType = ValueType.MULTI_CHOOSE
) {
    init {
        if (!canBeNone) {
            require(choices.isNotEmpty()) {
                "There are no values provided, " +
                    "but at least one must be selected. (required because by canBeNone = false)"
            }

            require(value.isNotEmpty()) {
                "There are no default values enabled, " +
                    "but at least one must be selected. (required because by canBeNone = false)"
            }
        }

        value.retainAll(choices)
    }

    private val choiceByName = choices.associateByTo(TreeMap(String.CASE_INSENSITIVE_ORDER)) { it.choiceName }

    override fun deserializeFrom(gson: Gson, element: JsonElement) {
        val active = get()
        active.clear()

        when (element) {
            is JsonArray -> element.forEach { active.tryToEnable(it.asString) }
            is JsonPrimitive -> active.tryToEnable(element.asString)
        }

        if (!canBeNone && active.isEmpty()) {
            active.addAll(choices)
        }

        set(active)
    }

    private fun MutableSet<T>.tryToEnable(name: String) {
        choiceByName[name]?.let { add(it) }
    }

    fun toggle(value: T): Boolean {
        require(value in choices) {
            "Provided value is not in the choices: $value"
        }

        val current = get()

        val isActive = value in current

        if (isActive) {
            if (!canBeNone && current.size <= 1) {
                return true
            }

            current.remove(value)
        } else {
            current.add(value)
        }

        // Trigger listeners
        set(current)

        return !isActive
    }

    operator fun contains(choice: T) = get().contains(choice)
}
