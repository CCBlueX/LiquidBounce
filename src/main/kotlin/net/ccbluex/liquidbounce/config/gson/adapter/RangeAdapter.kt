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

package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

object RangeAdapter : JsonSerializer<ClosedRange<*>>, JsonDeserializer<ClosedRange<*>> {

    private val TYPE_FLOAT_RANGE = (0.0f..5.0f).javaClass
    private val TYPE_DOUBLE_RANGE = (0.0..5.0).javaClass

    override fun serialize(src: ClosedRange<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()

        obj.add("from", context.serialize(src.start))
        obj.add("to", context.serialize(src.endInclusive))

        return obj
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?): ClosedRange<*> {
        val obj = json.asJsonObject

        val first = obj["from"]
        val second = obj["to"]

        return when (typeOfT) {
            TYPE_FLOAT_RANGE -> first.asFloat..second.asFloat

            TYPE_DOUBLE_RANGE -> first.asDouble..second.asDouble

            else -> throw JsonParseException("Unknown range type: $typeOfT for input $json")
        }
    }

}
