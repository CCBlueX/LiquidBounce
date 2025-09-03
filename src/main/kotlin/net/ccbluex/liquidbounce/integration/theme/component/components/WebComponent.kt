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

package net.ccbluex.liquidbounce.integration.theme.component.components

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment

class WebComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak> = emptyArray(),
    val values: Array<JsonObject> = emptyArray()
) : Component(name, enabled, alignment, tweaks) {

    override fun initConfigurable() {
        for (value in values) {
            configureConfigurable(this, value)
        }
        registerComponentListen(this)
        super.initConfigurable()
    }

}

/**
 * Assigns the value of the settings to the component
 *
 * A component can have dynamic settings which can be assigned through the JSON file
 * These have to be interpreted and assigned to the configurable
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
 * @param valueObject JsonObject
 */
@Suppress("LongMethod")
private fun configureConfigurable(configurable: Configurable, valueObject: JsonObject) {
    val type = valueObject["type"].asString
    val name = valueObject["name"].asString

    // todo: replace this with serious deserialization
    when (type) {
        "BOOLEAN" -> {
            val value = valueObject["value"].asBoolean
            configurable.boolean(name, value)
        }

        "INT" -> {
            val value = valueObject["value"].asInt
            val min = valueObject["range"].asJsonObject["min"].asInt
            val max = valueObject["range"].asJsonObject["max"].asInt
            val suffix = valueObject["suffix"]?.asString ?: ""
            configurable.int(name, value, min..max, suffix)
        }

        "INT_RANGE" -> {
            val valueMin = valueObject["value"].asJsonObject["min"].asInt
            val valueMax = valueObject["value"].asJsonObject["max"].asInt
            val min = valueObject["range"].asJsonObject["min"].asInt
            val max = valueObject["range"].asJsonObject["max"].asInt
            val suffix = valueObject["suffix"]?.asString ?: ""
            configurable.intRange(name, valueMin..valueMax, min..max, suffix)
        }

        "FLOAT" -> {
            val value = valueObject["value"].asFloat
            val min = valueObject["range"].asJsonObject["min"].asFloat
            val max = valueObject["range"].asJsonObject["max"].asFloat
            val suffix = valueObject["suffix"]?.asString ?: ""
            configurable.float(name, value, min..max, suffix)
        }

        "FLOAT_RANGE" -> {
            val valueMin = valueObject["value"].asJsonObject["min"].asFloat
            val valueMax = valueObject["value"].asJsonObject["max"].asFloat
            val min = valueObject["range"].asJsonObject["min"].asFloat
            val max = valueObject["range"].asJsonObject["max"].asFloat
            val suffix = valueObject["suffix"]?.asString ?: ""
            configurable.floatRange(name, valueMin..valueMax, min..max, suffix)
        }

        "TEXT" -> {
            val value = valueObject["value"].asString
            configurable.text(name, value)
        }

        "COLOR" -> {
            val value = valueObject["value"].asInt
            configurable.color(name, Color4b(value, hasAlpha = true))
        }

        "CONFIGURABLE" -> {
            val configurableObject = Configurable(name)
            val values = valueObject["values"].asJsonArray
            for (value in values) {
                configureConfigurable(configurableObject, value.asJsonObject)
            }
            configurable.tree(configurableObject)
        }
        // same as configurable but it is [ToggleableConfigurable]
        "TOGGLEABLE" -> {
            val value = valueObject["value"].asBoolean
            // Parent is NULL in that case because we are not dealing with Listenable anyway and only use it
            // as toggleable Configurable
            val configurableObject = object : ToggleableConfigurable(null, name, value) {}
            val settings = valueObject["values"].asJsonArray
            for (setting in settings) {
                configureConfigurable(configurableObject, setting.asJsonObject)
            }
            configurable.tree(configurableObject)
        }

        else -> error("Unsupported type: $type")
    }
}
