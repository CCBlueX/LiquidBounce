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

package net.ccbluex.liquidbounce.utils.input

import com.mojang.blaze3d.platform.InputConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InputUtilTest {

    private companion object {
        /**
         * Names that stand for "no key" and therefore resolve to [InputConstants.UNKNOWN] on purpose.
         */
        val UNBOUND_NAMES = setOf("none", "unknown")
    }

    @Test
    fun `inputByName resolves the documented name formats`() {
        assertResolves("key.keyboard.a", "a")
        assertResolves("key.keyboard.a", "key.keyboard.a")
        assertResolves("key.keyboard.left.shift", "left_shift")
        assertResolves("key.keyboard.left.shift", "key.keyboard.left.shift")
        assertResolves("key.keyboard.keypad.0", "keypad.0")
        assertResolves("key.mouse.left", "mouse.left")
        assertResolves("key.mouse.middle", "key.mouse.middle")
    }

    @Test
    fun `inputByName is case insensitive`() {
        assertResolves("key.keyboard.a", "A")
        assertResolves("key.keyboard.a", "KEYBOARD.A")
        assertResolves("key.keyboard.left.shift", "Left_Shift")
        assertResolves("key.mouse.left", "KEY.MOUSE.LEFT")
        assertResolves("key.mouse.left", "MOUSE.LEFT")
        assertResolves("key.mouse.left", "Mouse.Left")
        assertEquals(InputConstants.UNKNOWN, inputByName("NoNe"))
    }

    /**
     * Unnamed keys are looked up by their number, so `InputConstants.getKey` used to leave a
     * `NumberFormatException` for any name that is neither known nor numeric - including the
     * typos that `.bind` and `.binds add` are given by users.
     */
    @Test
    fun `inputByName returns unknown instead of throwing for unrecognized names`() {
        for (name in listOf("lshift", "ctrl", "qq", "space bar", "", "key.keyboard.middle")) {
            assertEquals(InputConstants.UNKNOWN, inputByName(name), "Resolved name '$name'")
        }
    }

    @Test
    fun `available key names report their own input type`() {
        assertTrue("a" in availableKeyboardKeys)
        assertTrue("left.shift" in availableKeyboardKeys)
        assertTrue(availableKeyboardKeys.none { it.startsWith("mouse.") }, "Mouse names in keyboard keys")

        assertTrue("mouse.left" in availableMouseKeys)
        assertTrue(availableMouseKeys.all { it.startsWith("mouse.") }, "Keyboard names in mouse keys")
    }

    /**
     * Every name suggested for the key argument of `.bind` / `.binds add` has to be bindable.
     */
    @Test
    fun `every suggested input key resolves to a key`() {
        val unresolved = availableInputKeys
            .filter { it !in UNBOUND_NAMES && inputByName(it) == InputConstants.UNKNOWN }

        assertEquals(emptyList(), unresolved, "Suggested names that cannot be bound")
    }

    private fun assertResolves(expectedName: String, input: String) {
        assertEquals(expectedName, inputByName(input).name, "Resolved name '$input'")
    }

}
