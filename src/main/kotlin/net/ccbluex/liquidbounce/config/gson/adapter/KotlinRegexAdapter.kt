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

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.regex.Pattern

object KotlinRegexAdapter : TypeAdapter<Regex>() {

    override fun read(source: JsonReader): Regex? =
        when (source.peek()) {
            JsonToken.NULL -> null
            JsonToken.STRING -> Pattern.compile(source.nextString()).toRegex()
            else -> error("Unexpected token ${source.peek()} for kotlin.text.Regex, should be ${JsonToken.STRING}")
        }

    override fun write(sink: JsonWriter, value: Regex?) {
        if (value == null) {
            sink.nullValue()
        } else {
            sink.value(value.pattern)
        }
    }
}
