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
package net.ccbluex.liquidbounce.utils.kotlin

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RequestHandlerTest {

    class TestClientModule(name: String) : ClientModule(name, Category.MISC, state = true) {
        override val running: Boolean
            get() = enabled
    }

    companion object {
        private val MODULE_1 = TestClientModule("module1")
        private val MODULE_2 = TestClientModule("module2")
        private val MODULE_3 = TestClientModule("module3")
        private val MODULE_4 = TestClientModule("module4")
    }

    @BeforeEach
    fun resetModules() {
        MODULE_1.enabled = true
        MODULE_2.enabled = true
        MODULE_3.enabled = true
        MODULE_4.enabled = true
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

        MODULE_1.enabled = false

        requestHandler.tick()

        assertNull(requestHandler.getActiveRequestValue())
    }
}
