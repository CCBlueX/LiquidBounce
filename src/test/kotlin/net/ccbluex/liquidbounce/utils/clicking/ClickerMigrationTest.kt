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
package net.ccbluex.liquidbounce.utils.clicking

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClickerMigrationTest {

    private fun old(technique: String) = JsonParser.parseString(
        """{"name":"Clicker","value":[
            {"name":"Technique","value":"$technique"},
            {"name":"CPS","value":{"from":8,"to":10}},
            {"name":"MaxPerTick","value":2}
        ]}"""
    ).asJsonObject.also(::migrateClickTechnique)

    private fun JsonObject.technique() = getAsJsonArray("value").map { it.asJsonObject }
        .first { it["name"].asString == "Technique" }

    private fun JsonObject.cps(mode: String) = technique().getAsJsonObject("choices").getAsJsonObject(mode)
        .getAsJsonArray("value").single().asJsonObject["value"].asJsonObject

    @Test
    fun `an old technique keeps its name and its cps`() {
        for (name in arrayOf("Human", "Constant")) {
            val clicker = old(name)
            assertEquals(name, clicker.technique()["active"].asString)
            assertEquals(8, clicker.cps("Human")["from"].asInt)
            assertEquals(10, clicker.cps("Constant")["to"].asInt)
        }
    }

    @Test
    fun `the removed AI technique becomes Human`() {
        assertEquals("Human", old("AI").technique()["active"].asString)
    }

    @Test
    fun `a migrated config is left alone`() {
        val clicker = old("Constant")
        val before = clicker.toString()
        migrateClickTechnique(clicker)
        assertEquals(before, clicker.toString())
        assertNull(clicker.technique()["value"].takeIf { it.isJsonPrimitive })
    }
}
