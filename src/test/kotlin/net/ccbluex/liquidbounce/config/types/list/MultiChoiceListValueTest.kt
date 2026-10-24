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

package net.ccbluex.liquidbounce.config.types.list

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultiChoiceListValueTest {

    private enum class Choice(override val tag: String) : Tagged {
        A("A"), B("B"), C("C")
    }

    companion object {
        init { MinecraftBootstrap.ensureInitialized() }
    }

    private fun value(
        defaults: MutableSet<Choice> = linkedSetOf(Choice.A),
        canBeNone: Boolean = true,
        ordered: Boolean = false,
    ) = MultiChoiceListValue("Choices", defaults, Choice.entries.toSet(), canBeNone, ordered)

    @Test
    fun `toggling preserves defaults and previously published selections`() {
        val defaults = linkedSetOf(Choice.A)
        val value = value(defaults)
        val previous = value.get()

        assertTrue(value.toggle(Choice.B))
        assertEquals(setOf(Choice.A), defaults)
        assertEquals(setOf(Choice.A), previous)
        assertEquals(setOf(Choice.A, Choice.B), value.get())

        value.restore()
        assertEquals(setOf(Choice.A), value.get())
    }

    @Test
    fun `deserialization preserves defaults for restore`() {
        val value = value()
        value.deserializeFrom(Gson(), JsonParser.parseString("[\"B\",\"C\"]"))
        assertEquals(setOf(Choice.B, Choice.C), value.get())

        value.restore()

        assertEquals(setOf(Choice.A), value.get())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `membership changes reach state flow collectors`() = runTest {
        val value = value()
        val observed = mutableListOf<Set<Choice>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            value.asStateFlow().collect { observed += it.toSet() }
        }

        value.toggle(Choice.B)
        value.toggle(Choice.A)

        assertEquals(listOf(setOf(Choice.A), setOf(Choice.A, Choice.B), setOf(Choice.B)), observed)
    }

    @Test
    fun `immutable selection rejects both toggles and deserialization`() {
        val value = value()
        value.immutable()
        var changed = 0
        value.onChanged { changed++ }

        assertFalse(value.toggle(Choice.B))
        assertTrue(value.toggle(Choice.A))
        value.deserializeFrom(Gson(), JsonParser.parseString("[\"C\"]"))

        assertEquals(setOf(Choice.A), value.get())
        assertEquals(0, changed)
    }

    @Test
    fun `a rejected toggle leaves the active selection intact`() {
        val value = value()
        value.onChange { require(Choice.B !in it); it }

        assertFalse(value.toggle(Choice.B))

        assertEquals(setOf(Choice.A), value.get())
        assertEquals(setOf(Choice.A), value.asStateFlow().value)
    }

    @Test
    fun `listener replacement becomes the active selection`() {
        val value = value()
        value.onChange { linkedSetOf(Choice.C) }

        assertFalse(value.toggle(Choice.B))

        assertEquals(setOf(Choice.C), value.get())
        assertEquals(setOf(Choice.C), value.asStateFlow().value)
    }

    @Test
    fun `ordered deserialization can reorder the same members`() {
        val value = value(linkedSetOf(Choice.A, Choice.B), ordered = true)
        value.deserializeFrom(Gson(), JsonParser.parseString("[\"B\",\"A\"]"))

        assertEquals(listOf(Choice.B, Choice.A), value.get().toList())
        value.restore()
        assertEquals(listOf(Choice.A, Choice.B), value.get().toList())
        value.toggle(Choice.C)
        assertEquals(listOf(Choice.A, Choice.B, Choice.C), value.get().toList())
        value.restore()
        assertEquals(listOf(Choice.A, Choice.B), value.get().toList())
    }

    @Test
    fun `copies retain sorted set ordering and comparator`() {
        val defaults = sortedSetOf(compareByDescending<Choice> { it.ordinal }, Choice.A)
        val value = value(defaults, ordered = true)

        value.toggle(Choice.B)
        value.toggle(Choice.C)

        assertEquals(listOf(Choice.C, Choice.B, Choice.A), value.get().toList())
        assertEquals(setOf(Choice.A), defaults)
    }

    @Test
    fun `required selections keep the last choice and fall back on unknown tags`() {
        val value = value(canBeNone = false)
        assertTrue(value.toggle(Choice.A))
        assertEquals(setOf(Choice.A), value.get())

        value.deserializeFrom(Gson(), JsonParser.parseString("[\"unknown\"]"))
        assertEquals(Choice.entries.toSet(), value.get())
        value.restore()
        assertEquals(setOf(Choice.A), value.get())
    }
}
