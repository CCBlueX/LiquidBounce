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

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.client.ClientContext

/**
 * Values are written field by field, and their field holds the value of the main client.
 * In any other client the value of that client takes its place.
 */
object ValueAdapter : TypeAdapterFactory {

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType
        if (!Value::class.java.isAssignableFrom(rawType) || ValueGroup::class.java.isAssignableFrom(rawType)) {
            return null
        }

        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(writer: JsonWriter, value: T) {
                if (ClientContext.current.isMain) {
                    delegate.write(writer, value)
                    return
                }

                val json = delegate.toJsonTree(value).asJsonObject
                if (json.has(VALUE)) {
                    json.add(VALUE, gson.toJsonTree((value as Value<*>).get()))
                }
                gson.toJson(json, writer)
            }

            override fun read(reader: JsonReader): T = delegate.read(reader)
        }
    }

    private const val VALUE = "value"

}
