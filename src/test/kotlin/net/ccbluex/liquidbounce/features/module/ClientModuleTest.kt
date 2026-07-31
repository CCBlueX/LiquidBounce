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
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ModuleToggleEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.inGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ClientModuleTest {

    private object TestEventListener : EventListener {
        override val running: Boolean = true
    }

    @Test
    fun `toggle outside game emits module toggle events`() {
        assertFalse(inGame)

        val module = ClientModule("Test", ModuleCategories.MISC)
        val states = mutableListOf<Boolean>()
        val eventHook = TestEventListener.handler<ModuleToggleEvent> { event ->
            if (event.moduleName == module.name) {
                states += event.enabled
            }
        }

        try {
            module.onToggled(true)
            module.onToggled(false)
        } finally {
            EventManager.unregisterEventHook(ModuleToggleEvent::class.java, eventHook)
        }

        assertEquals(listOf(true, false), states)
    }

}
