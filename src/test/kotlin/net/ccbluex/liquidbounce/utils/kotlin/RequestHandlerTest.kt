package net.ccbluex.liquidbounce.utils.kotlin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RequestHandlerTest {
    @Test
    fun testRequestHandler() {
        val requestHandler = RequestHandler<String>()

        assertNull(requestHandler.getActiveRequestValue())

        requestHandler.request(RequestHandler.Request(1, 0, "requestA"))

        assertEquals("requestA", requestHandler.getActiveRequestValue())

        requestHandler.tick()

        assertNull(requestHandler.getActiveRequestValue())

        requestHandler.request(RequestHandler.Request(3, 0, "requestB"))
        requestHandler.request(RequestHandler.Request(2, 1, "requestC"))
        requestHandler.request(RequestHandler.Request(1, 100, "requestD"))

        assertEquals("requestD", requestHandler.getActiveRequestValue())
        requestHandler.tick()

        assertEquals("requestC", requestHandler.getActiveRequestValue())
        requestHandler.tick()

        assertEquals("requestB", requestHandler.getActiveRequestValue())
        requestHandler.tick()

        assertNull(requestHandler.getActiveRequestValue())
    }
}
