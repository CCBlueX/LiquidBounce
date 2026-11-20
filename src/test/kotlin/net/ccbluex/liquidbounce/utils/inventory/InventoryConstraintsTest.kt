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
package net.ccbluex.liquidbounce.utils.inventory

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.Test
import kotlin.test.assertEquals

class InventoryConstraintsTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }
    }

    /** The stored ranges after [InventoryConstraints.prepareDeserialize], by name. */
    private fun prepared(json: String): Map<String, IntRange> {
        val stored = JsonParser.parseString(json).asJsonObject
        InventoryConstraints().prepareDeserialize(stored)
        return stored.getAsJsonArray("value").map { it.asJsonObject }
            .filter { it["value"] is JsonObject }
            .associate { it["name"].asString to it["value"].asJsonObject.let { r -> r["from"].asInt..r["to"].asInt } }
    }

    @Test
    fun `delays saved in ticks become milliseconds`() {
        val ranges = prepared(
            """{"name":"Constraints","value":[
                {"name":"StartDelay","value":{"from":1,"to":2}},
                {"name":"ClickDelay","value":{"from":2,"to":4}},
                {"name":"CloseDelay","value":{"from":0,"to":0}},
                {"name":"MissChance","value":{"from":3,"to":5}}
            ]}"""
        )

        assertEquals(50..100, ranges["StartDelay"])
        assertEquals(100..200, ranges["ClickDelay"])
        assertEquals(0..0, ranges["CloseDelay"])
        assertEquals(3..5, ranges["MissChance"])
    }

    @Test
    fun `a published config keeps its milliseconds`() {
        val ranges = prepared(publicGson.toJson(InventoryConstraints()))

        assertEquals(100..200, ranges["ClickDelay"])
    }

    @Test
    fun `settings the ClickGUI sends back keep their milliseconds`() {
        val ranges = prepared(interopGson.toJson(InventoryConstraints()))

        assertEquals(100..200, ranges["ClickDelay"])
    }

}
