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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.EventListener
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.BeforeTest
import kotlin.test.Test

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

    @BeforeTest
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

    @Test
    fun testClear() {
        val requestHandler = RequestHandler<String>()
        requestHandler.request(RequestHandler.Request(1000, 0, MODULE_1, "request"))
        requestHandler.tick(100)

        requestHandler.clear()

        assertNull(requestHandler.getActiveRequestValue())

        requestHandler.request(RequestHandler.Request(1, 0, MODULE_1, "newRequest"))
        assertEquals("newRequest", requestHandler.getActiveRequestValue())
        requestHandler.tick()
        assertNull(requestHandler.getActiveRequestValue())
    }

    @Test
    fun `reusing a request retains its relative lifetime`() {
        val handler = RequestHandler<String>()
        val request = RequestHandler.Request(2, 0, MODULE_1, "active")
        handler.tick(10)
        handler.request(request)
        assertEquals(2, request.expiresIn)
        handler.tick()
        handler.request(request)
        handler.tick()
        assertEquals("active", handler.getActiveRequestValue())
        handler.tick()
        assertNull(handler.getActiveRequestValue())
    }

    @Test
    fun `the same request can be submitted to handlers with different clocks`() {
        val first = RequestHandler<String>()
        val second = RequestHandler<String>()
        val request = RequestHandler.Request(2, 0, MODULE_1, "active")
        first.tick(10)
        second.tick(100)
        first.request(request)
        second.request(request)

        first.tick(2)
        second.tick(2)

        assertNull(first.getActiveRequestValue())
        assertNull(second.getActiveRequestValue())
        assertEquals(2, request.expiresIn)
    }

    @Test
    fun `a long request lifetime does not overflow its deadline`() {
        val handler = RequestHandler<String>()
        handler.tick(100)
        handler.request(RequestHandler.Request(Int.MAX_VALUE, 0, MODULE_1, "active"))

        assertEquals("active", handler.getActiveRequestValue())
        handler.tick(Int.MAX_VALUE - 1)
        assertEquals("active", handler.getActiveRequestValue())
        handler.tick()
        assertNull(handler.getActiveRequestValue())
    }

    @Test
    fun `short requests still expire when the clock crosses Int MAX VALUE`() {
        val handler = RequestHandler<String>()
        handler.tick(Int.MAX_VALUE - 1)
        handler.request(RequestHandler.Request(3, 0, MODULE_1, "active"))

        handler.tick(2)
        assertEquals("active", handler.getActiveRequestValue())
        handler.tick()
        assertNull(handler.getActiveRequestValue())
    }

    @Test
    fun `clearing the handler does not change a reusable request lifetime`() {
        val handler = RequestHandler<String>()
        val request = RequestHandler.Request(2, 0, MODULE_1, "active")
        handler.tick(100)
        handler.request(request)
        handler.clear()
        handler.request(request)
        handler.tick(2)

        assertNull(handler.getActiveRequestValue())
    }
}
