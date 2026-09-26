/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package net.ccbluex.liquidbounce.config.types.group

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.Test
import kotlin.test.assertEquals

class ValueGroupTraversalTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }
    }

    @Test
    fun `collectors include toggleable groups and every mode`() {
        val root = ValueGroup("Root")
        val direct = root.boolean("Direct", false)
        val toggle = root.tree(TestToggleable("Toggle")).apply {
            boolean("Nested", false)
        }
        val choice = root.modes(null, "Choice", { 0 }) { parent ->
            arrayOf(
                TestMode(parent, "First").apply { boolean("FirstValue", false) },
                TestMode(parent, "Second").apply { boolean("SecondValue", false) },
            )
        }
        root.walkKeyPath()

        assertEquals(
            listOf("Direct", "Toggle", "Enabled", "Nested", "Choice", "FirstValue", "SecondValue"),
            root.collectValuesRecursively().map(Value<*>::name).toList()
        )
        assertEquals(
            listOf("Root", "Toggle", "Choice", "First", "Second"),
            root.collectValueGroupsRecursively().map(ValueGroup::name).toList()
        )
        assertEquals(listOf("First", "Second"), choice.modes.map(Mode::name))
    }

    @Test
    fun `prefix collectors prune unrelated branches and ignore case`() {
        val root = ValueGroup("Root")
        root.tree(ValueGroup("Visible")).boolean("Setting", false)
        root.tree(ValueGroup("Hidden")).boolean("Other", false)
        root.walkKeyPath()

        assertEquals(
            listOf("Setting"),
            root.collectValuesRecursively("LIQUIDBOUNCE.ROOT.VISIBLE").map(Value<*>::name).toList()
        )
        assertEquals(
            listOf("Root", "Visible"),
            root.collectValueGroupsRecursively("liquidbounce.root.visible").map(ValueGroup::name).toList()
        )
    }

    private class TestToggleable(name: String) : ToggleableValueGroup(null, name, enabled = false)

    private class TestMode(
        override val parent: ModeValueGroup<*>,
        name: String,
    ) : Mode(name)
}
