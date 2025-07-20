package net.ccbluex.liquidbounce.config.types

import com.google.gson.Gson
import com.google.gson.JsonElement
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.utils.input.HumanInputDeserializer
import java.util.*

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

    init {
        require(value is List<*> || value is HashSet<*> || value is Set<*>) {
            "Inner value must be a List, HashSet or Set, but was ${value::class.java.name}"
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun setByString(string: String) {
        val deserializer = this.innerValueType.deserializer

        requireNotNull(deserializer) { "Cannot deserialize values of type ${this.innerValueType} yet." }

        set(HumanInputDeserializer.parseArray(string, deserializer) as T)
    }

    override fun deserializeFrom(gson: Gson, element: JsonElement) {
        val currValue = this.inner

        set(when (currValue) {
            is List<*> -> {
                element.asJsonArray.mapTo(
                    mutableListOf()
                ) { gson.fromJson(it, this.innerType) } as T
            }

            is HashSet<*> -> {
                element.asJsonArray.mapTo(
                    HashSet()
                ) { gson.fromJson(it, this.innerType) } as T
            }

            is Set<*> -> {
                element.asJsonArray.mapTo(
                    TreeSet()
                ) { gson.fromJson(it, this.innerType) } as T
            }

            else -> error("Unsupported collection type: ${currValue::class.java.name}")
        })
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
