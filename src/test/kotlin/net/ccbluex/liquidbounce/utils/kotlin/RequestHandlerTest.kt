package net.ccbluex.liquidbounce.utils.kotlin

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RequestHandlerTest {
    @Test
    fun testRequestHandler() {
        val module1 = Module("module1", Category.MISC)
        val module2 = Module("module2", Category.MISC)
        val module3 = Module("module3", Category.MISC)
        val module4 = Module("module4", Category.MISC)
        val requestHandler = RequestHandler<String>()

        assertNull(requestHandler.getActiveRequestValue())

        requestHandler.request(RequestHandler.Request(1, 0, module1, "requestA"))

        assertEquals("requestA", requestHandler.getActiveRequestValue())

        requestHandler.tick()

        assertNull(requestHandler.getActiveRequestValue())

        requestHandler.request(RequestHandler.Request(3, 0, module2, "requestB"))
        requestHandler.request(RequestHandler.Request(2, 1, module3, "requestC"))
        requestHandler.request(RequestHandler.Request(1, 100, module4, "requestD"))

        assertEquals("requestD", requestHandler.getActiveRequestValue())
        requestHandler.tick()

        assertEquals("requestC", requestHandler.getActiveRequestValue())
        requestHandler.tick()

        assertEquals("requestB", requestHandler.getActiveRequestValue())
        requestHandler.tick()

        assertNull(requestHandler.getActiveRequestValue())
    }
}
