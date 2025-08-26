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
 *
 *
 */

package net.ccbluex.liquidbounce.config.gson.serializer

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.ccbluex.liquidbounce.utils.render.Alignment
import java.lang.reflect.Type

object ReadOnlyComponentSerializer : JsonSerializer<Component> {

    override fun serialize(
        src: Component,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ) = JsonObject().apply {
        addProperty("name", src.name)
        add("settings", serializeReadOnly(src, context))
    }

    private fun serializeReadOnly(
        configurable: Configurable,
        context: JsonSerializationContext
    ): JsonObject = JsonObject().apply {
        for (v in configurable.inner) {
            add(v.name.toLowerCamelCase(), when (v) {
                is Alignment -> JsonPrimitive(v.toStyle())
                is Configurable -> serializeReadOnly(v, context)
                else -> context.serialize(v.inner)
            })
        }
    }

}
