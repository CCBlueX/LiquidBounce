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
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.resources.Identifier
import java.io.File
import java.util.function.Function

class SimpleStringTypeAdapter<T : Any>(
    val fromString: Function<String, T>,
    val toString: Function<T, String>,
) : TypeAdapter<T>() {
    override fun write(writer: JsonWriter, value: T?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(toString.apply(value))
        }
    }

    override fun read(reader: JsonReader): T? {
        return when (val token = reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            JsonToken.STRING -> fromString.apply(reader.nextString())
            else -> throw JsonParseException("Unexpected token $token for String input")
        }
    }

    companion object {
        @JvmField
        val FILE = SimpleStringTypeAdapter(::File, File::getPath)

        @JvmField
        val KT_REGEX = SimpleStringTypeAdapter(::Regex) { it.pattern }

        @JvmField
        val INPUT_KEY = SimpleStringTypeAdapter(InputConstants::getKey, InputConstants.Key::getName)

        @JvmField
        val IDENTIFIER = SimpleStringTypeAdapter(Identifier::parse, Any::toString)
    }

}
