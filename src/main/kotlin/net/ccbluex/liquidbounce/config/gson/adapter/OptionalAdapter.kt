/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 202 CCBlueX
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

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.ParameterizedType
import java.util.*

class OptionalAdapter<T : Any> private constructor(private val adapter: TypeAdapter<T>) : TypeAdapter<Optional<T>>() {
    override fun write(sink: JsonWriter, value: Optional<T>?) {
        adapter.write(sink, value?.orElse(null))
    }

    override fun read(source: JsonReader): Optional<T>? {
        val peek = source.peek()
        return if (peek != JsonToken.NULL) {
            Optional.ofNullable(adapter.read(source))
        } else {
            source.skipValue()
            Optional.empty()
        }
    }

    companion object Factory : TypeAdapterFactory {
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            if (type.getRawType() != Optional::class.java) {
                return null
            }
            val parameterizedType = type.type as ParameterizedType
            val actualType = parameterizedType.actualTypeArguments[0]
            val adapter = gson.getAdapter(TypeToken.get(actualType))
            @Suppress("UNCHECKED_CAST")
            return OptionalAdapter(adapter) as TypeAdapter<T>
        }
    }
}
