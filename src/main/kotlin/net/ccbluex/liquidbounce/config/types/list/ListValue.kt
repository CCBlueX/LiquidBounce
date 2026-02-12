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
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import net.ccbluex.fastutil.enumMapOf
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.utils.input.HumanInputDeserializer
import java.util.SequencedSet

open class ListValue<T : MutableCollection<E>, E>(
    name: String,
    /**
     * Enabled values. A mutable and unordered [Set].
     */
    value: T,

    /**
     * Not the type of [value] but the type of list.
     */
    valueType: ValueType = ValueType.LIST,

    /**
     * Used to determine the type of the inner value.
     */
    @Exclude val innerValueType: ValueType = ValueType.INVALID,

    /**
     * Used to deserialize the [value] from JSON.
     * TODO: Might replace [innerType] with a [Class] variable
     *   from the inner value type in the future.
     */
    @Exclude @ProtocolExclude val innerType: Class<E>,

    ) : Value<T>(
    name,
    defaultValue = value,
    valueType = valueType,
) {

    @Suppress("UNCHECKED_CAST")
    final override fun setByString(string: String) {
        val deserializer = this.innerValueType.deserializer

        requireNotNull(deserializer) { "Cannot deserialize values of type ${this.innerValueType} yet." }

        set(HumanInputDeserializer.parseArray(string, deserializer) as T)
    }

    final override fun deserializeFrom(gson: Gson, element: JsonElement) {
        val currValue = this.inner
        if (element is JsonArray) {
            val newItems = Array(element.size()) {
                gson.fromJson(element[it], this.innerType)
            }
            currValue.clear()
            currValue.addAll(newItems)
        } else {
            val newItem = gson.fromJson(element, this.innerType)
            currValue.clear()
            currValue.add(newItem)
        }

        set(currValue) { /** Trigger listener callbacks */ }
    }

}

/**
 * This allows users to input any kind of [E] value,
 * so it might not deserialize correctly if the input cannot be
 * converted to the [innerType].
 *
 * TODO: Implement support for input validation in the UI.
 */
open class MutableListValue<T : MutableCollection<E>, E>(
    name: String,
    value: T,
    innerValueType: ValueType = ValueType.INVALID,
    innerType: Class<E>,
) : ListValue<T, E>(
    name,
    value,
    ValueType.MUTABLE_LIST,
    innerValueType,
    innerType
)

open class ItemListValue<T : MutableSet<E>, E>(
    name: String,
    value: T,
    @Exclude var items: Set<NamedItem<E>>,
    innerValueType: ValueType = ValueType.INVALID,
    innerType: Class<E>,
) : ListValue<T, E>(
    name,
    value,
    ValueType.NAMED_ITEM_LIST,
    innerValueType,
    innerType
) {

    init {
        require(items.isNotEmpty()) {
            "ItemListValue must have at least one item defined."
        }
    }

    data class NamedItem<T>(
        val name: String,
        val value: T,
        val icon: String? = null
    )

}

/**
 *
 */
class RegistryListValue<T : SequencedSet<E>, E>(
    name: String,
    value: T,
    innerValueType: ValueType = ValueType.INVALID,
    innerType: Class<E>,
) : ListValue<T, E>(
    name,
    value,
    ValueType.REGISTRY_LIST,
    innerValueType,
    innerType
) {

    /**
     * This is used to determine the registry endpoint for the API.
     */
    @Exclude
    val registry: String =
        VALUE_TYPE_TO_REGISTRY_NAME[innerValueType] ?: error("Unsupported registry type: $innerValueType")

}

private val VALUE_TYPE_TO_REGISTRY_NAME = enumMapOf(
    ValueType.BLOCK, "block",
    ValueType.ITEM, "item",
    ValueType.SOUND_EVENT, "sound_event",
    ValueType.MOB_EFFECT, "mob_effect",
    ValueType.C2S_PACKET, "c2s_packet",
    ValueType.S2C_PACKET, "s2c_packet",
    ValueType.ENTITY_TYPE, "entity_type",
    ValueType.ENCHANTMENT, "enchantment",
    ValueType.MENU, "menu",
    ValueType.CLIENT_MODULE, "client_module",
)
