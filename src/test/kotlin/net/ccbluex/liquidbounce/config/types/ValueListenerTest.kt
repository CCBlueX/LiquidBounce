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

package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.Test
import kotlin.test.assertEquals

class ValueListenerTest {

    companion object {
        init { MinecraftBootstrap.ensureInitialized() }
    }

    @Test
    fun `normalizers compose in registration order`() {
        val value = Value("Name", defaultValue = "old", valueType = ValueType.TEXT)
        val inputs = mutableListOf<String>()
        value.onChange { it.trim() }
        value.onChange { inputs += it; it.lowercase() }
        value.onChange { inputs += it; "[$it]" }
        var changed: String? = null
        value.onChanged { changed = it }

        value.set("  NEW  ")

        assertEquals(listOf("NEW", "new"), inputs)
        assertEquals("[new]", value.get())
        assertEquals("[new]", changed)
        assertEquals("[new]", value.asStateFlow().value)
    }

    @Test
    fun `validation receives normalized input`() {
        val value = Value("Name", defaultValue = "old", valueType = ValueType.TEXT)
        value.onChange { it.trim() }
        value.onChange { require(it.length <= 3); it }

        value.set("  new  ")

        assertEquals("new", value.get())
    }

    @Test
    fun `rejected normalized input leaves stored state and notifications unchanged`() {
        val value = Value("Name", defaultValue = "old", valueType = ValueType.TEXT)
        value.onChange { it.trim() }
        value.onChange { require(it.length <= 3); it }
        var changes = 0
        value.onChanged { changes++ }

        value.set("  too long  ")

        assertEquals("old", value.get())
        assertEquals("old", value.asStateFlow().value)
        assertEquals(0, changes)
    }

    @Test
    fun `explicit apply callback receives the composed value`() {
        val value = Value("Name", defaultValue = "old", valueType = ValueType.TEXT)
        value.onChange { it.trim() }
        value.onChange { it.lowercase() }
        var applied: String? = null

        value.set("  NEW  ") { applied = it }

        assertEquals("new", applied)
        assertEquals("new", value.asStateFlow().value)
    }
}
