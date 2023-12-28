/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.config.adapter

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import java.lang.reflect.Type

object ConfigurableSerializer : JsonSerializer<Configurable> {

    override fun serialize(
        src: Configurable,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val obj = JsonObject()

        obj.addProperty("name", src.name)
        obj.add("value", context.serialize(src.value))

        return obj
    }

}

object ProtocolConfigurableSerializer : JsonSerializer<Configurable> {

    override fun serialize(
        src: Configurable,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val obj = JsonObject()

        obj.addProperty("name", src.name)
        obj.add("value", context.serialize(src.value.filter {
            !it.notAnOption
        }))
        obj.add("valueType", context.serialize(src.valueType))

        return obj
    }

}

/**
 * Assign [AutoConfigurableSerializer] serializer for Configurables that should be published publicly instead of
 * using [ConfigurableSerializer]
 */
object AutoConfigurableSerializer : JsonSerializer<Configurable> {

    override fun serialize(
        src: Configurable,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val obj = JsonObject()

        obj.addProperty("name", src.name)
        obj.add("value", context.serialize(src.value.filter { checkIfInclude(it) }))

        return obj
    }

    /**
     * Checks if value should be included in public config
     */
    private fun checkIfInclude(value: Value<*>): Boolean {
        /**
         * Do not include values that are not supposed to be shared
         * with other users
         */
        if (value.doNotInclude) {
            return false
        }

        // Might check if value is module
        if (value is Module) {
            /**
             * Do not include modules that are heavily user-personalised
             */
            if (value.category == Category.RENDER) {
                return false
            }
        }

        // Otherwise include value
        return true
    }

}
