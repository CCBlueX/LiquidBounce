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
package net.ccbluex.liquidbounce.features.misc.proxy

import net.ccbluex.liquidbounce.utils.io.readText
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OkHttpUrlConnectionTest {

    @Test
    fun `successful responses expose their body and metadata`() {
        val body = TrackingBody("pack")
        val connection = connection(200, "OK", body)
        try {
            assertEquals(200, connection.responseCode)
            assertEquals("OK", connection.responseMessage)
            assertEquals(4L, connection.contentLengthLong)
            assertEquals("application/zip", connection.getHeaderField("Content-Type"))
            assertNull(connection.errorStream)
            assertEquals("pack", connection.inputStream.use { it.readText() })
        } finally {
            connection.disconnect()
        }
        assertTrue(body.closed)
    }

    @Test
    fun `not found and gone throw FileNotFoundException while preserving the error body`() {
        for (status in intArrayOf(404, 410)) {
            val body = TrackingBody("missing pack")
            val connection = connection(status, "Missing", body)
            try {
                val failure = assertFailsWith<FileNotFoundException> { connection.inputStream }
                assertEquals("https://example.invalid/pack.zip", failure.message)
                assertEquals(status, connection.responseCode)
                assertFalse(body.closed)
                assertEquals("missing pack", connection.errorStream!!.use { it.readText() })
            } finally {
                connection.disconnect()
            }
            assertTrue(body.closed)
        }
    }

    @Test
    fun `other HTTP errors fail the download instead of exposing an error page as a pack`() {
        for (status in intArrayOf(400, 403, 429, 500, 503)) {
            val body = TrackingBody("error page")
            val connection = connection(status, "Failed", body)
            try {
                val failure = assertFailsWith<IOException> { connection.inputStream }
                assertFalse(failure is FileNotFoundException)
                assertTrue(failure.message!!.contains(status.toString()))
                assertEquals(status, connection.responseCode)
                assertEquals("Failed", connection.responseMessage)
            } finally {
                connection.disconnect()
            }
            assertTrue(body.closed)
        }
    }

    @Test
    fun `disconnect closes unread error bodies`() {
        for (status in intArrayOf(404, 410, 503)) {
            val body = TrackingBody("unread error")
            val connection = connection(status, "Failed", body)
            try {
                assertFailsWith<IOException> { connection.inputStream }
                assertFalse(body.closed)
            } finally {
                connection.disconnect()
            }
            assertTrue(body.closed)
        }
    }

    @Test
    fun `HTTP2 errors remain readable when the response message is empty`() {
        val body = TrackingBody("error")
        val connection = connection(503, "", body, Protocol.HTTP_2)
        try {
            val failure = assertFailsWith<IOException> { connection.inputStream }
            assertEquals("", connection.responseMessage)
            assertTrue(failure.message!!.contains("503"))
            assertEquals("error", connection.errorStream!!.use { it.readText() })
        } finally {
            connection.disconnect()
        }
        assertTrue(body.closed)
    }

    @Test
    fun `asking for an error stream does not initiate a request`() {
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            assertEquals("test-agent", chain.request().header("User-Agent"))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(500).message("Failed").body(TrackingBody("error")).build()
        }.build()
        val connection = OkHttpUrlConnection(URL("https://example.invalid/pack.zip"), client)
        connection.setRequestProperty("User-Agent", "test-agent")
        try {
            assertNull(connection.errorStream)
            assertEquals(0, calls)
            assertEquals(500, connection.responseCode)
            assertEquals("error", connection.errorStream!!.use { it.readText() })
            assertEquals("Failed", connection.responseMessage)
            assertEquals(1, calls)
        } finally {
            connection.disconnect()
        }
    }

    private fun connection(
        status: Int,
        message: String,
        body: ResponseBody,
        protocol: Protocol = Protocol.HTTP_1_1,
    ): OkHttpUrlConnection {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(protocol)
                .code(status).message(message).header("Content-Type", "application/zip").body(body).build()
        }.build()
        return OkHttpUrlConnection(URL("https://example.invalid/pack.zip"), client)
    }

    private class TrackingBody(private val text: String) : ResponseBody() {
        var closed = false
            private set
        private val content = object : ForwardingSource(Buffer().writeUtf8(text)) {
            override fun close() {
                closed = true
                super.close()
            }
        }.buffer()

        override fun contentType(): MediaType? = null
        override fun contentLength(): Long = text.length.toLong()
        override fun source() = content
    }
}
