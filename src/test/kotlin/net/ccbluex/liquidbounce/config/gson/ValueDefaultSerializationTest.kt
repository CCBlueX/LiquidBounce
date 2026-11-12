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

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ValueDefaultSerializationTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }
    }

    private fun ValueGroup.interopValue(name: String) = JsonParser.parseString(interopGson.toJson(this)).asJsonObject
        .getAsJsonArray("value").first { it.asJsonObject["name"].asString == name }.asJsonObject

    private fun ValueGroup.fileValue(name: String) = JsonParser.parseString(fileGson.toJson(this)).asJsonObject
        .getAsJsonArray("value").first { it.asJsonObject["name"].asString == name }.asJsonObject

    @Test
    fun `the gui gets the default next to the value`() {
        val root = ValueGroup("Root")
        val range = root.int("Range", 3, 0, 10)
        range.set(7)

        val json = root.interopValue("Range")
        assertEquals(7, json["value"].asInt)
        assertEquals(3, json["defaultValue"].asInt)
    }

    @Test
    fun `the default of a list does not follow its edits`() {
        val root = ValueGroup("Root")
        val names = root.mutableList("Names", mutableListOf("a", "b"), ValueType.TEXT)
        names.get().add("c")

        val json = root.interopValue("Names")
        assertEquals(listOf("a", "b", "c"), json.getAsJsonArray("value").map { it.asString })
        assertEquals(listOf("a", "b"), json.getAsJsonArray("defaultValue").map { it.asString })
    }

    @Test
    fun `files do not store the default`() {
        val root = ValueGroup("Root")
        root.int("Range", 3, 0, 10)

        assertFalse(root.fileValue("Range").has("defaultValue"))
    }

}
