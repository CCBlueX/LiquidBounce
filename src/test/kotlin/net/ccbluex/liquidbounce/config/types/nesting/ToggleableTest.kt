/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.config.types.nesting

import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

class ToggleableTest {
    private lateinit var testToggleable: TestToggleableImpl

    @BeforeEach
    fun setUp() {
        testToggleable = TestToggleableImpl()
    }

    @Test
    fun `onToggled with true state should call onEnabled and return true`() {
        // Reset state before test
        testToggleable.resetFlags()

        // Call onToggled with true
        val result = testToggleable.onToggled(true)

        // Verify onEnabled was called, onDisabled was not called
        assertEquals(true, testToggleable.enabledCalled)
        assertEquals(false, testToggleable.disabledCalled)
        // Verify the method returns the correct state
        assertEquals(true, result)
    }

    @Test
    fun `onToggled with false state should call onDisabled and return false`() {
        // Reset state before test
        testToggleable.resetFlags()

        // Call onToggled with false
        val result = testToggleable.onToggled(false)

        // Verify onDisabled was called, onEnabled was not called
        assertEquals(false, testToggleable.enabledCalled)
        assertEquals(true, testToggleable.disabledCalled)
        // Verify the method returns the correct state
        assertEquals(false, result)
    }

    @Test
    fun `toggle multiple times should call corresponding methods each time`() {
        // Reset state before test
        testToggleable.resetFlags()

        // Toggle to true
        testToggleable.onToggled(true)
        assertEquals(true, testToggleable.enabledCalled)
        assertEquals(false, testToggleable.disabledCalled)

        // Toggle to false
        testToggleable.resetFlags()
        testToggleable.onToggled(false)
        assertEquals(false, testToggleable.enabledCalled)
        assertEquals(true, testToggleable.disabledCalled)

        // Toggle to true again
        testToggleable.resetFlags()
        testToggleable.onToggled(true)
        assertEquals(true, testToggleable.enabledCalled)
        assertEquals(false, testToggleable.disabledCalled)
    }

    /**
     * A test implementation of Toggleable that tracks whether onEnabled and onDisabled were called.
     */
    private class TestToggleableImpl : Toggleable {
        override var enabled: Boolean = false
        var enabledCalled = false
        var disabledCalled = false

        override fun onEnabled() {
            enabledCalled = true
            enabled = true
        }

        override fun onDisabled() {
            disabledCalled = true
            enabled = false
        }

        fun resetFlags() {
            enabledCalled = false
            disabledCalled = false
        }
    }
}
