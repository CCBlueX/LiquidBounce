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

package net.ccbluex.liquidbounce.config.util

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.authlib.utils.boolean
import net.ccbluex.liquidbounce.authlib.utils.string
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.render.engine.type.Color4b

fun interface JsonValueFactory {
    fun Configurable.attachFrom(valueObject: JsonObject)

    companion object {
        @JvmField
        val Boolean = JsonValueFactory { valueObject ->
            boolean(
                name = valueObject.nameOrError(),
                default = valueObject.boolean("value") ?: false,
            )
        }

        @JvmField
        val Float = JsonValueFactory { valueObject ->
            val name = valueObject.nameOrError()
            val value = valueObject["value"].asFloat
            val min = valueObject["range"].asJsonObject["min"].asFloat
            val max = valueObject["range"].asJsonObject["max"].asFloat
            val suffix = valueObject["suffix"]?.asString ?: ""
            float(name, value, min..max, suffix)
        }

        @JvmField
        val FloatRange = JsonValueFactory { valueObject ->
            val name = valueObject.nameOrError()
            val valueMin = valueObject["value"].asJsonObject["min"].asFloat
            val valueMax = valueObject["value"].asJsonObject["max"].asFloat
            val min = valueObject["range"].asJsonObject["min"].asFloat
            val max = valueObject["range"].asJsonObject["max"].asFloat
            val suffix = valueObject["suffix"]?.asString ?: ""
            floatRange(name, valueMin..valueMax, min..max, suffix)
        }

        @JvmField
        val Int = JsonValueFactory { valueObject ->
            val name = valueObject.nameOrError()
            val value = valueObject["value"].asInt
            val min = valueObject["range"].asJsonObject["min"].asInt
            val max = valueObject["range"].asJsonObject["max"].asInt
            val suffix = valueObject["suffix"]?.asString ?: ""
            int(name, value, min..max, suffix)
        }

        @JvmField
        val IntRange = JsonValueFactory { valueObject ->
            val name = valueObject.nameOrError()
            val valueMin = valueObject["value"].asJsonObject["min"].asInt
            val valueMax = valueObject["value"].asJsonObject["max"].asInt
            val min = valueObject["range"].asJsonObject["min"].asInt
            val max = valueObject["range"].asJsonObject["max"].asInt
            val suffix = valueObject["suffix"]?.asString ?: ""
            intRange(name, valueMin..valueMax, min..max, suffix)
        }

        @JvmField
        val Text = JsonValueFactory { valueObject ->
            val name = valueObject.nameOrError()
            val value = valueObject["value"].asString
            text(name, value)
        }

        @JvmField
        val Color = JsonValueFactory { valueObject ->
            val name = valueObject.nameOrError()
            val value = valueObject["value"].asInt
            color(name, Color4b(value, hasAlpha = true))
        }

        @JvmField
        val Configurable = JsonValueFactory { valueObject ->
            val name = valueObject.nameOrError()
            val subConfigurable = Configurable(name)
            val values = valueObject["values"].asJsonArray
            for (value in values) {
                subConfigurable.json(value.asJsonObject)
            }
            tree(subConfigurable)
        }

        @JvmField
        val Toggleable = JsonValueFactory { valueObject ->
            val name = valueObject.nameOrError()
            val value = valueObject["value"].asBoolean
            // Parent is NULL in that case because we are not dealing with Listenable anyway and only use it
            // as toggleable Configurable
            val subConfigurable = object : ToggleableConfigurable(
                parent = null, name, enabled = value
            ) {}
            val settings = valueObject["values"].asJsonArray
            for (setting in settings) {
                subConfigurable.json(setting.asJsonObject)
            }
            tree(subConfigurable)
        }



        @Suppress("NOTHING_TO_INLINE")
        private inline fun JsonObject.nameOrError(): String = string("name") ?: error("No name")

    }

}
