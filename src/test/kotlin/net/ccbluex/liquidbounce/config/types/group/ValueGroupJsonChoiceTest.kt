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

package net.ccbluex.liquidbounce.config.types.group

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSerializer
import net.ccbluex.liquidbounce.config.gson.serializer.ValueGroupSerializer
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ValueGroupJsonChoiceTest {

    companion object {
        init {
            // Value deserialization touches vanilla registries before a game instance exists.
            MinecraftBootstrap.ensureInitialized()
        }
    }

    @Test
    fun `json choice creates modes with isolated settings`() {
        val valueGroup = ValueGroup("Test")
        val choiceDefinition = JsonParser.parseString(
            """
            {
              "type": "CHOICE",
              "name": "Source",
              "value": "URL",
              "choices": [
                {
                  "name": "URL",
                  "values": [
                    { "type": "TEXT", "name": "URL", "value": "https://example.com/image.png" }
                  ]
                },
                {
                  "name": "File",
                  "values": [
                    { "type": "BOOLEAN", "name": "Local", "value": true }
                  ]
                }
              ]
            }
            """.trimIndent()
        ).asJsonObject

        valueGroup.json(choiceDefinition)

        val choice = assertIs<ModeValueGroup<*>>(valueGroup.inner.single())
        assertEquals(ValueType.CHOICE, choice.valueType)
        assertEquals("URL", choice.activeMode.name)
        assertEquals(listOf("URL", "File"), choice.modes.map(Mode::name))
        assertEquals(ValueType.TEXT, choice.modes[0].inner.single().valueType)
        assertEquals(ValueType.BOOLEAN, choice.modes[1].inner.single().valueType)

        choice.setByString("File")
        assertEquals("File", choice.activeMode.name)

        val readOnlyGson = GsonBuilder()
            .registerTypeHierarchyAdapter(ValueGroup::class.java, JsonSerializer<ValueGroup> { source, _, context ->
                ValueGroupSerializer.serializeReadOnly(source, context)
            })
            .create()
        val readOnlyChoice = readOnlyGson.toJsonTree(valueGroup).asJsonObject["source"].asJsonObject

        assertEquals("File", readOnlyChoice["active"].asString)
        assertEquals(true, readOnlyChoice["value"].asJsonObject["local"].asBoolean)
    }
}
