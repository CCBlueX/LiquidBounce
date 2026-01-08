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
package net.ccbluex.liquidbounce.utils.kotlin

import net.ccbluex.liquidbounce.event.EventListener
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RequestHandlerTest {

    private class TestEventListener(val name: String) : EventListener {
        override var running: Boolean = true
    }

    companion object {
        private val MODULE_1 = TestEventListener("module1")
        private val MODULE_2 = TestEventListener("module2")
        private val MODULE_3 = TestEventListener("module3")
        private val MODULE_4 = TestEventListener("module4")
    }

    @BeforeEach
    fun resetModules() {
        MODULE_1.running = true
        MODULE_2.running = true
        MODULE_3.running = true
        MODULE_4.running = true
    }

    @Test
    fun testRequestHandler() {
        val requestHandler = RequestHandler<String>()

        assertNull(requestHandler.getActiveRequestValue())

        requestHandler.request(RequestHandler.Request(1000, -1, MODULE_1, "requestA"))
        requestHandler.request(RequestHandler.Request(3, 0, MODULE_2, "requestB"))
        requestHandler.request(RequestHandler.Request(2, 1, MODULE_3, "requestC"))
        requestHandler.request(RequestHandler.Request(1, 100, MODULE_4, "requestD"))

        assertEquals("requestD", requestHandler.getActiveRequestValue())
        requestHandler.tick()

        assertEquals("requestC", requestHandler.getActiveRequestValue())
        requestHandler.tick()

        assertEquals("requestB", requestHandler.getActiveRequestValue())
        requestHandler.tick()

        assertEquals("requestA", requestHandler.getActiveRequestValue())
        requestHandler.tick()

        MODULE_1.running = false

        requestHandler.tick()

        assertNull(requestHandler.getActiveRequestValue())
    }
}
