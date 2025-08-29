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
package net.ccbluex.liquidbounce.config.gson

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.config.gson.adapter.OptionalAdapter
import java.util.*
import kotlin.test.*

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
}
