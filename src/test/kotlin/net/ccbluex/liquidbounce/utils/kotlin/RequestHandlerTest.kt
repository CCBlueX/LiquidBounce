/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.features.module.Module
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RequestHandlerTest {
    companion object {
        private val MODULE_1 = Module("module1", Category.MISC, state = true)
        private val MODULE_2 = Module("module2", Category.MISC, state = true)
        private val MODULE_3 = Module("module3", Category.MISC, state = true)
        private val MODULE_4 = Module("module4", Category.MISC, state = true)
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
