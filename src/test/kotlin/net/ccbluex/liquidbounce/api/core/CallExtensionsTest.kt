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
package net.ccbluex.liquidbounce.api.core

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.Timeout
import okio.buffer
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CallExtensionsTest {

    @Test
    fun `cancelling the returned future cancels the call`() {
        for (mayInterrupt in listOf(false, true)) {
            val call = TestCall()
            val future = call.enqueueAsFuture()

            assertTrue(future.cancel(mayInterrupt))

            assertTrue(future.isCancelled)
            assertEquals(1, call.cancelCount)
            assertFailsWith<CancellationException> { future.join() }
        }
    }

    @Test
    fun `successful response remains open until the caller closes it`() {
        val call = TestCall()
        val future = call.enqueueAsFuture()
        val body = TrackingBody()
        val response = response(call, body)

        call.callback.onResponse(call, response)

        assertSame(response, future.join())
        assertFalse(body.closed)
        assertEquals(0, call.cancelCount)
        future.join().close()
        assertTrue(body.closed)
    }

    @Test
    fun `failure preserves the original exception without cancelling the call`() {
        val call = TestCall()
        val future = call.enqueueAsFuture()
        val failure = IOException("Connection failed")

        call.callback.onFailure(call, failure)

        assertSame(failure, assertFailsWith<CompletionException> { future.join() }.cause)
        assertFalse(future.isCancelled)
        assertEquals(0, call.cancelCount)
    }

    @Test
    fun `response arriving after cancellation is closed`() {
        val call = TestCall()
        val future = call.enqueueAsFuture()
        val body = TrackingBody()
        future.cancel(false)

        call.callback.onResponse(call, response(call, body))

        assertTrue(body.closed)
        assertTrue(future.isCancelled)
        assertEquals(1, call.cancelCount)
    }

    @Test
    fun `failure arriving after cancellation does not replace cancellation`() {
        val call = TestCall()
        val future = call.enqueueAsFuture()
        future.cancel(false)

        call.callback.onFailure(call, IOException("Canceled"))

        assertTrue(future.isCancelled)
        assertEquals(1, call.cancelCount)
        assertFailsWith<CancellationException> { future.join() }
    }

    @Test
    fun `cancellation after successful completion leaves the call and response alone`() {
        val call = TestCall()
        val future = call.enqueueAsFuture()
        val body = TrackingBody()
        val response = response(call, body)
        call.callback.onResponse(call, response)

        assertFalse(future.cancel(false))

        assertSame(response, future.join())
        assertFalse(body.closed)
        assertEquals(0, call.cancelCount)
        response.close()
    }

    private fun response(call: Call, body: ResponseBody): Response = Response.Builder()
        .request(call.request())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body)
        .build()

    private class TrackingBody : ResponseBody() {
        var closed = false
            private set
        private val content = object : ForwardingSource(Buffer().writeUtf8("body")) {
            override fun close() {
                closed = true
                super.close()
            }
        }.buffer()

        override fun contentType(): MediaType? = null
        override fun contentLength(): Long = 4
        override fun source() = content
    }

    private class TestCall : Call by OkHttpClient().newCall(
        Request.Builder().url("https://example.invalid/").build()
    ) {
        lateinit var callback: Callback
        var cancelCount = 0
            private set
        private var executed = false

        override fun enqueue(responseCallback: Callback) {
            check(!executed)
            executed = true
            callback = responseCallback
        }
        override fun cancel() {
            cancelCount++
        }
        override fun isExecuted(): Boolean = executed
        override fun isCanceled(): Boolean = cancelCount > 0
        override fun timeout(): Timeout = Timeout.NONE
        override fun clone(): Call = TestCall()
        override fun execute(): Response = error("Only asynchronous calls are supported by this test")
    }
}
