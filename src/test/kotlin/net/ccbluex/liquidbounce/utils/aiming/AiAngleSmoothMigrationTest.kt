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
package net.ccbluex.liquidbounce.utils.aiming

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.migrateAiAngleSmooth
import kotlin.test.Test
import kotlin.test.assertEquals

class AiAngleSmoothMigrationTest {
    private fun JsonObject.names() = getAsJsonArray("value").map { it.asJsonObject["name"].asString }

    @Test
    fun `the old model choice goes and Correction becomes Assist`() {
        val ai = JsonParser.parseString(
            """{"name":"AI","value":[
                {"name":"Model","active":"19KC8KP","value":[],"choices":{"19KC8KP":{"name":"19KC8KP","value":[]}}},
                {"name":"Correction","active":"Linear","value":[],"choices":{}},
                {"name":"OutputMultiplier","value":[]}
            ]}"""
        ).asJsonObject
        migrateAiAngleSmooth(ai)
        assertEquals(listOf("Assist", "OutputMultiplier"), ai.names())
        assertEquals("Linear", ai.getAsJsonArray("value")[0].asJsonObject["active"].asString)
    }

    @Test
    fun `a migrated config is left alone`() {
        val ai = JsonParser.parseString(
            """{"name":"AI","value":[
                {"name":"Model","value":"Juggle"},
                {"name":"Assist","active":"None","value":[],"choices":{}},
                {"name":"Correction","active":"Linear","value":[],"choices":{}}
            ]}"""
        ).asJsonObject
        val before = ai.toString()
        migrateAiAngleSmooth(ai)
        assertEquals(before, ai.toString())
    }
}
