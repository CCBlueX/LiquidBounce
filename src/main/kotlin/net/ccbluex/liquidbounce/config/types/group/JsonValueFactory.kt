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

package net.ccbluex.liquidbounce.config.types.group

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.config.types.FileDialogMode
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.group.ValueGroup.ModeBuilder
import net.ccbluex.liquidbounce.config.types.list.Tagged.Companion.asTagged
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import java.io.File
import kotlin.collections.mapTo

private fun ValueGroup.modes(
    name: String,
    default: String,
    modes: Map<String, ModeBuilder>,
): ModeValueGroup<Mode> {
    require(modes.isNotEmpty()) { "Mode group '$name' must contain at least one mode." }
    class SimpleMode(name: String, override val parent: ModeValueGroup<*>) : Mode(name)

    return modes(
        eventListener = null,
        name = name,
        activeCallback = { modes ->
            val idx = modes.indexOfFirst { it.name == default }

            check(idx != -1) {
                "The active choice $default is not contained within the choice array" +
                    " (${modes.joinToString { it.name }})"
            }

            idx
        },
        modesCallback = { parent ->
            modes.entries.mapToArray { (modeName, configure) ->
                val mode = SimpleMode(modeName, parent)

                with(configure) {
                    mode.build()
                }
                mode
            }
        },
    )
}

/**
 * Assigns the value of the settings to the component
 *
 * A component can have dynamic settings which can be assigned through the JSON file
 * These have to be interpreted and assigned to the value group
 *
 * An example:
 * {
 *     "type": "INT",
 *     "name": "Size",
 *     "value": 14,
 *     "range": {
 *         "min": 1,
 *         "max": 100
 *     },
 *     "suffix": "px"
 * }
 *
 * TODO: Replace with proper deserialization
 *
 * @param valueObject JsonObject
 */
@Suppress("LongMethod", "CognitiveComplexMethod")
fun ValueGroup.json(valueObject: JsonObject) {
    val type = enumValueOf<ValueType>(valueObject["type"].asString)
    val name = valueObject["name"].asString

    // todo: replace this with serious deserialization
    when (type) {
        ValueType.BOOLEAN -> {
            val value = valueObject["value"].asBoolean
            boolean(name, value)
        }

        ValueType.INT -> {
            val value = valueObject["value"].asInt
            val min = valueObject["range"].asJsonObject["min"].asInt
            val max = valueObject["range"].asJsonObject["max"].asInt
            val suffix = valueObject["suffix"]?.asString ?: ""
            int(name, value, min..max, suffix)
        }

        ValueType.INT_RANGE -> {
            val valueMin = valueObject["value"].asJsonObject["min"].asInt
            val valueMax = valueObject["value"].asJsonObject["max"].asInt
            val min = valueObject["range"].asJsonObject["min"].asInt
            val max = valueObject["range"].asJsonObject["max"].asInt
            val suffix = valueObject["suffix"]?.asString ?: ""
            intRange(name, valueMin..valueMax, min..max, suffix)
        }

        ValueType.FLOAT -> {
            val value = valueObject["value"].asFloat
            val min = valueObject["range"].asJsonObject["min"].asFloat
            val max = valueObject["range"].asJsonObject["max"].asFloat
            val suffix = valueObject["suffix"]?.asString ?: ""
            float(name, value, min..max, suffix)
        }

        ValueType.FLOAT_RANGE -> {
            val valueMin = valueObject["value"].asJsonObject["min"].asFloat
            val valueMax = valueObject["value"].asJsonObject["max"].asFloat
            val min = valueObject["range"].asJsonObject["min"].asFloat
            val max = valueObject["range"].asJsonObject["max"].asFloat
            val suffix = valueObject["suffix"]?.asString ?: ""
            floatRange(name, valueMin..valueMax, min..max, suffix)
        }

        ValueType.TEXT -> {
            val value = valueObject["value"].asString
            text(name, value)
        }

        ValueType.FILE -> {
            val value = valueObject["value"]
                ?.takeUnless { it is JsonNull }
                ?.asString?.takeUnless(String::isBlank)
                ?.let(::File)
            val dialogMode = valueObject["dialogMode"]
                ?.takeUnless { it is JsonNull }
                ?.asString
                ?.let(FileDialogMode::valueOf)
                ?: FileDialogMode.OPEN_FILE
            val supportedExtensions = valueObject["supportedExtensions"]
                ?.takeUnless { it is JsonNull }
                ?.asJsonArray
                ?.mapTo(linkedSetOf()) { it.asString }

            file(name, value, dialogMode, supportedExtensions)
        }

        ValueType.COLOR -> {
            val value = valueObject["value"].asInt
            color(name, Color4b(value))
        }

        ValueType.CONFIGURABLE -> {
            val subValueGroup = ValueGroup(name)
            val values = valueObject["values"].asJsonArray
            for (value in values) {
                subValueGroup.json(value.asJsonObject)
            }
            tree(subValueGroup)
        }
        // same as value group but it is [ToggleableValueGroup]
        ValueType.TOGGLEABLE -> {
            val value = valueObject["value"].asBoolean
            // Parent is NULL in that case because we are not dealing with Listenable anyway and only use it
            // as toggleable ValueGroup
            val subValueGroup = object : ToggleableValueGroup(null, name, value) {}
            val settings = valueObject["values"].asJsonArray
            for (setting in settings) {
                subValueGroup.json(setting.asJsonObject)
            }
            tree(subValueGroup)
        }

        ValueType.CHOOSE -> {
            val value = valueObject["value"].asString.asTagged()
            val choices = valueObject["choices"].asJsonArray.mapTo(linkedSetOf()) { it.asString.asTagged() }

            enumChoice(name, value, choices)
        }

        ValueType.CHOICE -> {
            val value1 = valueObject["value"].asString
            val modes1 = valueObject["choices"].asJsonArray.associateTo(linkedMapOf()) { choiceElement ->
                val choiceObject = choiceElement.asJsonObject
                val choiceName = choiceObject["name"].asString
                val settings = choiceObject["values"]?.asJsonArray ?: emptyList()
                val configure = ValueGroup.ModeBuilder {
                    for (setting in settings) {
                        json(setting.asJsonObject)
                    }
                }

                choiceName to configure
            }
            modes(name, value1, modes1)
        }

        ValueType.MULTI_CHOOSE -> {
            fun parseBoolean(key: String, default: Boolean) = when (val json = valueObject[key]) {
                null, is JsonNull -> default
                is JsonPrimitive, is JsonArray -> json.asBoolean
                else -> error("Unexpected JSON value (${json.javaClass}): $json, should be boolean")
            }

            val canBeNone = parseBoolean(key = "canBeNone", default = true)
            val isOrderSensitive = parseBoolean(key = "isOrderSensitive", default = false)

            val value = valueObject["value"].asJsonArray.mapTo(
                if (isOrderSensitive) linkedSetOf() else sortedSetOf()
            ) { it.asString.asTagged() }
            val choices = valueObject["choices"].asJsonArray.mapTo(linkedSetOf()) { it.asString.asTagged() }

            multiEnumChoice(name, default = value, choices, canBeNone, isOrderSensitive)
        }

        ValueType.REGISTRY_LIST -> {
            val innerValueType = enumValueOf<ValueType>(valueObject["innerValueType"].asString)
            val normalizedValue = when (val value = valueObject["value"]) {
                is JsonArray -> value
                is JsonPrimitive -> listOf(value)
                null, is JsonNull -> emptyList()
                else -> error("Unexpected JSON value (${value.javaClass}): $value, should be Identifier list")
            }

            when (innerValueType) {
                ValueType.BLOCK -> {
                    blocks(name, normalizedValue.mapTo(blockSortedSetOf()) {
                        publicGson.fromJson(it, Block::class.java)
                    })
                }

                ValueType.ITEM -> {
                    items(name, normalizedValue.mapTo(itemSortedSetOf()) {
                        publicGson.fromJson(it, Item::class.java)
                    })
                }

                else -> error("Unsupported inner value type for ${ValueType.REGISTRY_LIST}: $innerValueType")
            }
        }

        else -> error("Unsupported type: $type")
    }
}
