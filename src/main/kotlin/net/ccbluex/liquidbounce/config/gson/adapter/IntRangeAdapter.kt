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

package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.*
import java.lang.reflect.Type

object IntRangeAdapter : JsonSerializer<IntRange>, JsonDeserializer<IntRange> {

    override fun serialize(src: IntRange, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()

        obj.addProperty("from", src.first)
        obj.addProperty("to", src.last)

        return obj
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): IntRange {
        if (json.isJsonPrimitive) {
            val primitive = json.asJsonPrimitive
            if (!primitive.isNumber) {
                throw JsonParseException("Expected number, got ${primitive.asString}")
            }

            return IntRange(primitive.asInt, primitive.asInt)
        }

        if (!json.isJsonObject) {
            throw JsonParseException("Expected object, got ${json.javaClass.name}")
        }

        val obj = json.asJsonObject
        return obj["from"].asInt..obj["to"].asInt
    }

}
