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

import com.google.gson.Gson
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.Test
import kotlin.test.assertEquals

class ValueFlagsSerializationTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }
    }

    private var shown = true

    private val root = ValueGroup("Root").apply {
        boolean("Stored", false)
        boolean("Transient", false).notPersistent()
        boolean("Conditional", false).visibleWhen { shown }
    }

    private fun Gson.names(group: ValueGroup) = JsonParser.parseString(toJson(group)).asJsonObject
        .getAsJsonArray("value").map { it.asJsonObject["name"].asString }

    @Test
    fun `files and public configs leave out values that are not persistent`() {
        assertEquals(listOf("Stored", "Conditional"), fileGson.names(root))
        assertEquals(listOf("Stored", "Conditional"), publicGson.names(root))
    }

    @Test
    fun `the gui gets transient values but not hidden ones`() {
        assertEquals(listOf("Stored", "Transient", "Conditional"), interopGson.names(root))
        shown = false
        assertEquals(listOf("Stored", "Transient"), interopGson.names(root))
    }

}
