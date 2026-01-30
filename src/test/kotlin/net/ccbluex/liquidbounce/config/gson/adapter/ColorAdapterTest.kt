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

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ColorAdapterTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(Color4b::class.java, ColorAdapter)
        .create()

    @Test
    fun `test serialize Color4b`() {
        val color = Color4b(255, 0, 0, 255) // Red
        val json = gson.toJson(color)
        assertEquals(color.argb.toString(), json)
    }

    @Test
    fun `test serialize null Color4b`() {
        val color: Color4b? = null
        val json = gson.toJson(color)
        assertEquals("null", json)
    }

    @Test
    fun `test deserialize number to Color4b`() {
        val color = Color4b(0, 255, 0, 255) // Green
        val json = color.argb.toString()
        val deserialized = gson.fromJson(json, Color4b::class.java)
        assertEquals(color, deserialized)
    }

    @Test
    fun `test deserialize hex string to Color4b`() {
        val json = "\"#FF0000\"" // Red
        val deserialized = gson.fromJson(json, Color4b::class.java)
        assertEquals(Color4b(255, 0, 0, 255), deserialized)
    }

    @Test
    fun `test deserialize hex string with alpha to Color4b`() {
        val json = "\"#80FF0000\"" // Red with 50% alpha (approx)
        val deserialized = gson.fromJson(json, Color4b::class.java)
        assertEquals(Color4b(255, 0, 0, 128), deserialized)
    }

    @Test
    fun `test deserialize hex string without # to Color4b`() {
        val json = "\"FF0000\"" // Red
        val deserialized = gson.fromJson(json, Color4b::class.java)
        assertEquals(Color4b(255, 0, 0, 255), deserialized)
    }

    @Test
    fun `test deserialize null to Color4b`() {
        val json = "null"
        val deserialized = gson.fromJson(json, Color4b::class.java)
        assertNull(deserialized)
    }

    @Test
    fun `test deserialize invalid format throws exception`() {
        val json = "true"
        assertFailsWith<JsonParseException> {
            gson.fromJson(json, Color4b::class.java)
        }
    }
}
