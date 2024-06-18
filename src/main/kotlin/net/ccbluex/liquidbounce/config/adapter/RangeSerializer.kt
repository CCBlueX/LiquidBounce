/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import com.google.gson.*
import java.lang.reflect.Type

object RangeSerializer : JsonSerializer<ClosedRange<*>>, JsonDeserializer<ClosedRange<*>> {

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

        if (typeOfT == (0.0f..5.0f).javaClass) {
            return first.asFloat..second.asFloat
        } else if (typeOfT == (0.0..5.0).javaClass) {
            return first.asDouble..second.asDouble
        }

        throw IllegalArgumentException("Not implemented")
    }

}
