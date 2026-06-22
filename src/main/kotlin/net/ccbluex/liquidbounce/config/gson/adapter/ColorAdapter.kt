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

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import net.ccbluex.liquidbounce.render.engine.type.Color4b

object ColorAdapter : TypeAdapter<Color4b>() {
    override fun write(
        writer: JsonWriter,
        value: Color4b?,
    ) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.argb)
        }
    }

    override fun read(reader: JsonReader): Color4b? {
        return when (val token = reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            // Use nextLong().toInt() to safely handle unsigned 32-bit integers in JSON
            JsonToken.NUMBER -> Color4b(reader.nextLong().toInt())
            JsonToken.STRING -> Color4b.fromHex(reader.nextString())
            else -> {
                reader.skipValue()
                throw JsonParseException("Only number or hex format string can be parsed as color, found $token")
            }
        }
    }
}
