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
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.utils.input.HumanInputDeserializer

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
    override fun setByString(string: String) {
        val deserializer = this.innerValueType.deserializer

        requireNotNull(deserializer) { "Cannot deserialize values of type ${this.innerValueType} yet." }

        set(HumanInputDeserializer.parseArray(string, deserializer) as T)
    }

    override fun deserializeFrom(gson: Gson, element: JsonElement) {
        // TODO: Might add adaptation for single element like : ["foo", "bar"] or "foo"
        element as? JsonArray ?: error("ListValue can only be deserialized from a JsonArray.")

        val currValue = this.inner

        currValue.clear()
        element.mapTo(currValue) { gson.fromJson(it, this.innerType) }

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
    ValueType.ITEM_LIST,
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

open class RegistryListValue<T : MutableSet<E>, E>(
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
    @Exclude var registry: String = when (innerValueType) {
        ValueType.BLOCK -> "blocks"
        ValueType.ITEM -> "items"
        ValueType.SOUND -> "sounds"
        ValueType.STATUS_EFFECT -> "statuseffects"
        ValueType.CLIENT_PACKET -> "clientpackets"
        ValueType.SERVER_PACKET -> "serverpackets"
        else -> error("Unsupported registry type: $innerValueType")
    }

}
