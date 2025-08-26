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

package net.ccbluex.liquidbounce.config.gson.serializer

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import java.lang.reflect.Type

class ConfigurableSerializer(
    private val withValueType: Boolean, private val includePrivate: Boolean, private val includeNotAnOption: Boolean
) : JsonSerializer<Configurable> {

    companion object {

        /**
         * This serializer is used to serialize [Configurable]s to JSON
         */
        val FILE_SERIALIZER = ConfigurableSerializer(
            withValueType = false, includePrivate = true, includeNotAnOption = true
        )

        /**
         * This serializer is used to serialize [Configurable]s to JSON for interop communication
         */
        val INTEROP_SERIALIZER = ConfigurableSerializer(
            withValueType = true, includePrivate = true, includeNotAnOption = false
        )

        /**
         * This serializer is used to serialize [Configurable]s to JSON for public config
         */
        val PUBLIC_SERIALIZER = ConfigurableSerializer(
            withValueType = false, includePrivate = false, includeNotAnOption = true
        )

    }

    override fun serialize(
        src: Configurable, typeOfSrc: Type, context: JsonSerializationContext
    ) = JsonObject().apply {
        addProperty("name", src.name)
        add(
            "value",
            context.serialize(src.inner.filter { includeNotAnOption || !it.notAnOption }
                .filter { includePrivate || checkIfInclude(it) }))
        if (withValueType) {
            add("valueType", context.serialize(src.valueType))
        }
    }

    /**
     * Checks if value should be included in public config
     */
    private fun checkIfInclude(value: Value<*>): Boolean {
        /**
         * Do not include values that are not supposed to be shared
         * with other users
         */
        if (value.doNotInclude()) {
            return false
        }

        // Might check if value is module
        if (value is ClientModule) {
            /**
             * Do not include modules that are heavily user-personalised
             */
            if (value.category == Category.RENDER || value.category == Category.CLIENT ||
                value.category == Category.FUN) {
                return false
            }
        }

        // Otherwise include value
        return true
    }

}
