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
package net.ccbluex.liquidbounce.config.gson

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.ccbluex.liquidbounce.config.gson.adapter.OptionalAdapter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import java.util.*
import kotlin.test.assertNotNull

class OptionalAdapterTest {

    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(OptionalAdapter)
        .create()

    @Test
    fun `test serialize non-empty Optional`() {
        val optional = Optional.of("Hello")
        val json = gson.toJson(optional, object : TypeToken<Optional<String>>() {}.type)
        assertEquals("\"Hello\"", json)
    }

    @Test
    fun `test serialize empty Optional`() {
        val optional = Optional.empty<String>()
        val json = gson.toJson(optional, object : TypeToken<Optional<String>>() {}.type)
        assertEquals("null", json)
    }

    @Test
    fun `test deserialize non-null JSON to Optional`() {
        val json = "\"World\""
        val optional = gson.fromJson<Optional<String>>(json, object : TypeToken<Optional<String>>() {}.type)
        assertTrue(optional.isPresent)
        assertEquals("World", optional.get())
    }

    @Test
    fun `test deserialize null JSON to Optional`() {
        val json = "null"
        val optional = gson.fromJson<Optional<String>>(json, object : TypeToken<Optional<String>>() {}.type)
        assertNotNull(optional)
        assertFalse(optional.isPresent)
    }

    @Test
    fun `test round-trip Optional serialization and deserialization`() {
        val original = Optional.of("RoundTrip")
        val json = gson.toJson(original, object : TypeToken<Optional<String>>() {}.type)
        val deserialized = gson.fromJson<Optional<String>>(json, object : TypeToken<Optional<String>>() {}.type)
        assertEquals(original, deserialized)
    }

    @Test
    fun `test round-trip with empty Optional`() {
        val original = Optional.empty<String>()
        val json = gson.toJson(original, object : TypeToken<Optional<String>>() {}.type)
        val deserialized = gson.fromJson<Optional<String>>(json, object : TypeToken<Optional<String>>() {}.type)
        assertEquals(original, deserialized)
    }

    @Test
    fun `serialize Optional without an explicit type`() {
        assertEquals("\"Hello\"", gson.toJson(Optional.of("Hello")))
    }

    @Test
    fun `serialize empty Optional without an explicit type`() {
        assertEquals("null", gson.toJson(Optional.empty<String>()))
    }

    @Test
    fun `deserialize a raw Optional using the object adapter`() {
        val optional = gson.fromJson("{\"message\":\"Hello\"}", Optional::class.java)

        assertEquals(mapOf("message" to "Hello"), optional.get())
    }

    @Test
    fun `deserialize null into a raw Optional`() {
        val optional = gson.fromJson("null", Optional::class.java)

        assertEquals(Optional.empty<Any>(), optional)
    }

    @Test
    fun `empty Optional does not pass null to the element adapter`() {
        val customGson = gsonWithNonNullStringAdapter()
        val type = object : TypeToken<Optional<String>>() {}.type

        assertEquals("null", customGson.toJson(Optional.empty<String>(), type))
    }

    @Test
    fun `null Optional does not pass null to the element adapter`() {
        val customGson = gsonWithNonNullStringAdapter()
        val type = object : TypeToken<Optional<String>>() {}.type

        assertEquals("null", customGson.toJson(null, type))
    }

    @Test
    fun `present Optional uses the registered element adapter`() {
        val customGson = gsonWithNonNullStringAdapter()
        val type = object : TypeToken<Optional<String>>() {}.type

        assertEquals("\"custom:Hello\"", customGson.toJson(Optional.of("Hello"), type))
        assertEquals(Optional.of("Hello"), customGson.fromJson("\"custom:Hello\"", type))
    }

    private fun gsonWithNonNullStringAdapter() = GsonBuilder()
        .registerTypeAdapterFactory(OptionalAdapter)
        .registerTypeAdapter(String::class.java, object : TypeAdapter<String>() {
            override fun write(sink: JsonWriter, value: String?) {
                sink.value("custom:${requireNotNull(value)}")
            }

            override fun read(source: JsonReader): String = source.nextString().removePrefix("custom:")
        })
        .create()
}
