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

@file:Suppress("TooManyFunctions", "NOTHING_TO_INLINE")

package net.ccbluex.liquidbounce.config.gson.util

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import net.ccbluex.liquidbounce.config.gson.publicGson
import org.apache.commons.io.input.CharSequenceReader
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset

/**
 * Read JSON content
 */
inline fun <reified T> CharSequence.readJson(): T =
    CharSequenceReader(this).readJson()

/**
 * Read JSON content from a [File].
 */
inline fun <reified T> File.readJson(charset: Charset = Charsets.UTF_8): T =
    inputStream().readJson(charset)

/**
 * Read JSON content from an [InputStream] and close it
 */
inline fun <reified T> InputStream.readJson(charset: Charset = Charsets.UTF_8): T =
    bufferedReader(charset).readJson()

/**
 * Read JSON content from a [Reader] and close it
 */
inline fun <reified T> Reader.readJson(gson: Gson = publicGson): T = use {
    gson.fromJson(it, object : TypeToken<T>() {}.type)
}

inline fun JsonReader.parseTree(): JsonElement = JsonParser.parseReader(this)

inline fun <reified T> JsonDeserializationContext.deserialize(json: JsonElement): T =
    deserialize(json, object : TypeToken<T>() {}.type)

fun jsonArrayOf(vararg elements: JsonElement) = JsonArray(elements.size).apply {
    elements.forEach { add(it) }
}

class JsonArrayBuilder(initialCapacity: Int) {
    private val backend = JsonArray(initialCapacity)

    operator fun JsonElement.unaryPlus() {
        backend.add(this)
    }

    fun build() = backend
}

inline fun jsonArray(
    initialCapacity: Int = 10,
    builderAction: JsonArrayBuilder.() -> Unit
) = JsonArrayBuilder(initialCapacity).apply(builderAction).build()

@JvmInline
value class JsonObjectBuilder(private val backend: JsonObject) {

    operator fun String.invoke(value: JsonElement?) {
        backend.add(this, value)
    }

    operator fun String.invoke(value: Char?) {
        backend.addProperty(this, value)
    }

    operator fun String.invoke(value: Number?) {
        backend.addProperty(this, value)
    }

    operator fun String.invoke(value: String?) {
        backend.addProperty(this, value)
    }

    operator fun String.invoke(value: Boolean?) {
        backend.addProperty(this, value)
    }

    inline operator fun String.invoke(builderAction: JsonObjectBuilder.() -> Unit) {
        invoke(jsonObject(builderAction))
    }

    /**
     * Fallback
     */
    operator fun String.invoke(value: Any?) {
        when (value) {
            null -> backend.add(this, JsonNull.INSTANCE)
            is String -> backend.addProperty(this, value)
            is Number -> backend.addProperty(this, value)
            is Boolean -> backend.addProperty(this, value)
            is JsonElement -> backend.add(this, value)
            is JsonObjectBuilder -> backend.add(this, value.build())
            else -> throw IllegalArgumentException("Unsupported type: ${value::class.java}")
        }
    }

    fun build() = backend
}

inline fun jsonObject(
    builderAction: JsonObjectBuilder.() -> Unit
) = JsonObjectBuilder(JsonObject()).apply(builderAction).build()

inline fun JsonArray.getOrNull(index: Int): JsonElement? =
    if (index in 0 until this.size()) this[index] else null
