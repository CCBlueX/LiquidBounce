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

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.config.gson.util

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.config.gson.publicGson
import java.io.InputStream
import java.io.Reader

/**
 * Decode JSON content
 */
inline fun <reified T> decode(stringJson: String): T =
    stringJson.reader().use(::decode)

/**
 * Decode JSON content from an [InputStream] and close it
 */
inline fun <reified T> decode(inputStream: InputStream): T =
    inputStream.bufferedReader().use(::decode)

/**
 * Decode JSON content from a [Reader] and close it
 */
inline fun <reified T> decode(reader: Reader): T = reader.use {
    publicGson.fromJson(reader, object : TypeToken<T>() {}.type)
}

// Never add elements to it!
private val EMPTY_JSON_ARRAY = JsonArray(0)
private val EMPTY_JSON_OBJECT = JsonObject()

internal fun emptyJsonArray(): JsonArray = EMPTY_JSON_ARRAY
internal fun emptyJsonObject(): JsonObject = EMPTY_JSON_OBJECT

fun String.toJsonPrimitive(): JsonPrimitive = JsonPrimitive(this)
fun Char.toJsonPrimitive(): JsonPrimitive = JsonPrimitive(this)
fun Number.toJsonPrimitive(): JsonPrimitive = JsonPrimitive(this)
fun Boolean.toJsonPrimitive(): JsonPrimitive = JsonPrimitive(this)

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

class JsonObjectBuilder {
    private val backend = JsonObject()

    infix fun String.to(value: JsonElement) {
        backend.add(this, value)
    }

    infix fun String.to(value: Char) {
        backend.addProperty(this, value)
    }

    infix fun String.to(value: Number) {
        backend.addProperty(this, value)
    }

    infix fun String.to(value: String) {
        backend.addProperty(this, value)
    }

    infix fun String.to(value: Boolean) {
        backend.addProperty(this, value)
    }

    /**
     * Fallback
     */
    infix fun String.to(value: Any?) {
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

inline fun json(
    builderAction: JsonObjectBuilder.() -> Unit
) = JsonObjectBuilder().apply(builderAction).build()
