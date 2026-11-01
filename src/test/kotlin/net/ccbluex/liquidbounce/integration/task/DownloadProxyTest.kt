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
package net.ccbluex.liquidbounce.integration.task

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.ServerSocket
import java.net.URI
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class DownloadProxyTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `idle selection expires after five seconds and input cancels the timer`() {
        val start = TimeUnit.SECONDS.toNanos(10)
        val timeout = ProxySelectionTimeout(start)
        assertEquals(5L, timeout.secondsRemaining(start))
        assertEquals(1L, timeout.secondsRemaining(start + TimeUnit.SECONDS.toNanos(4)))
        assertFalse(timeout.hasExpired(start + TimeUnit.SECONDS.toNanos(4)))
        assertTrue(timeout.hasExpired(start + TimeUnit.SECONDS.toNanos(5)))
        val interacted = ProxySelectionTimeout(start)
        interacted.interact()
        assertEquals(null, interacted.secondsRemaining(start))
        assertFalse(interacted.hasExpired(start + TimeUnit.SECONDS.toNanos(30)))
    }

    @Test
    fun `proxy settings survive restarts and first launch prompts only when needed`() {
        val path = directory.resolve("settings").resolve("startup-proxy.properties")
        assertEquals(null, DownloadProxyStorage.load(path))
        assertTrue(DownloadProxyStorage.shouldPrompt(null, false, false, false))
        assertFalse(DownloadProxyStorage.shouldPrompt(null, false, false, true))
        val custom = DownloadProxy.Choice.Custom(DownloadProxy.parseAddress("localhost:8080"))
        val choices = listOf(DownloadProxy.Choice.Direct, DownloadProxy.Choice.System, custom,
            custom.copy(type = Proxy.Type.SOCKS))
        for (choice in choices) {
            DownloadProxyStorage.save(path, choice)
            assertEquals(choice, DownloadProxyStorage.load(path))
        }
        assertTrue(DownloadProxyStorage.shouldPrompt(custom, false, false, false))
        assertFalse(DownloadProxyStorage.shouldPrompt(custom, true, false, false))
        assertTrue(DownloadProxyStorage.shouldPrompt(custom, true, true, false))
        assertFalse(DownloadProxyStorage.shouldPrompt(DownloadProxy.Choice.System, true, false, false))
        Files.writeString(path, "mode=custom\naddress=user:password@example.org:80\n")
        assertEquals(null, DownloadProxyStorage.load(path))
    }

    @Test
    fun `proxy routes both download clients and restores the JVM selector`() {
        val original = ProxySelector.getDefault()
        ServerSocket(0).use { server ->
            server.soTimeout = 5000
            val received = CompletableFuture.supplyAsync {
                repeat(2) {
                    server.accept().use { socket ->
                        socket.soTimeout = 5000
                        val reader = socket.getInputStream().bufferedReader()
                        while (!reader.readLine().isNullOrEmpty()) Unit
                        val response = "HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nOK"
                        socket.getOutputStream().write(response.toByteArray())
                    }
                }
            }
            val address = DownloadProxy.parseAddress("127.0.0.1:${server.localPort}")
            DownloadProxy.choice = DownloadProxy.Choice.Custom(address)
            try {
                val selector = DownloadProxy.selector()
                assertEquals(Proxy.Type.DIRECT, selector.select(URI("http://localhost/test")).single().type())
                val client = OkHttpClient.Builder().proxySelector(selector).build()
                client.newCall(Request.Builder().url("http://example.invalid/test").build()).execute().use {
                    assertEquals("OK", it.body.string())
                }
                DownloadProxy.withJavaDownloadProxy {
                    val connection = URI("http://example.invalid/test").toURL().openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    assertEquals("OK", connection.getInputStream().bufferedReader().use { it.readText() })
                }
                received.get(5, TimeUnit.SECONDS)
                assertSame(original, ProxySelector.getDefault())
            } finally {
                DownloadProxy.choice = DownloadProxy.Choice.Direct
            }
        }
    }

    @Test
    fun `invalid addresses are rejected and download scope is restored`() {
        assertThrows(Exception::class.java) { DownloadProxy.parseAddress("user:password@example.org:80") }
        assertThrows(Exception::class.java) { DownloadProxy.parseAddress("example.org:99999") }
        assertFalse(DownloadProxy.wantsDownloadClient)
        assertThrows(IllegalStateException::class.java) {
            DownloadProxy.withDownloadClient {
                assertTrue(DownloadProxy.wantsDownloadClient)
                error("failure")
            }
        }
        assertFalse(DownloadProxy.wantsDownloadClient)
    }

    @Test
    fun `system mode delegates and Java proxy does not affect other threads`() {
        val original = ProxySelector.getDefault()
        val selected = DownloadProxy.Choice.Custom(InetSocketAddress.createUnresolved("proxy.example", 8080))
        val uri = URI("https://example.invalid/")
        assertEquals(original.select(uri), DownloadProxy.selector(DownloadProxy.Choice.System, original).select(uri))
        DownloadProxy.choice = selected
        try {
            DownloadProxy.withJavaDownloadProxy {
                val unrelated = CompletableFuture.supplyAsync { ProxySelector.getDefault().select(uri) }
                assertEquals(original.select(uri), unrelated.get(5, TimeUnit.SECONDS))
                assertEquals(Proxy.Type.HTTP, ProxySelector.getDefault().select(uri).single().type())
            }
            assertSame(original, ProxySelector.getDefault())
        } finally {
            DownloadProxy.choice = DownloadProxy.Choice.Direct
        }
    }

    @Test
    fun `system proxy helper runs without JVM launch options`() {
        val result = SystemProxySelector().select(URI("https://example.invalid/private?token=secret"))
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `custom SOCKS is used for both HTTP and HTTPS destinations`() {
        val choice = DownloadProxy.Choice.Custom(
            InetSocketAddress.createUnresolved("proxy.example", 1080), Proxy.Type.SOCKS
        )
        val selector = DownloadProxy.selector(choice)
        for (scheme in listOf("http", "https")) {
            assertEquals(Proxy.Type.SOCKS, selector.select(URI("$scheme://example.invalid/")).single().type())
        }
    }

    @Test
    fun `HTTPS downloads use CONNECT through an HTTP proxy`() {
        ServerSocket(0).use { server ->
            server.soTimeout = 5000
            val requestLine = CompletableFuture.supplyAsync {
                server.accept().use { socket ->
                    socket.soTimeout = 5000
                    val reader = socket.getInputStream().bufferedReader()
                    val line = reader.readLine()
                    while (!reader.readLine().isNullOrEmpty()) Unit
                    val response = "HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n"
                    socket.getOutputStream().write(response.toByteArray())
                    line
                }
            }
            val choice = DownloadProxy.Choice.Custom(DownloadProxy.parseAddress("127.0.0.1:${server.localPort}"))
            val client = OkHttpClient.Builder().proxySelector(DownloadProxy.selector(choice)).build()
            assertThrows(IOException::class.java) {
                client.newCall(Request.Builder().url("https://example.invalid/test").build()).execute().close()
            }
            assertTrue(requestLine.get(5, TimeUnit.SECONDS).startsWith("CONNECT example.invalid:443 "))
        }
    }

    @Test
    fun `SOCKS proxy forwards download clients`() {
        val original = ProxySelector.getDefault()
        ServerSocket(0).use { server ->
            server.soTimeout = 5000
            val received = CompletableFuture.runAsync {
                repeat(2) {
                    server.accept().use { socket ->
                        socket.soTimeout = 5000
                        val input = socket.getInputStream()
                        val output = socket.getOutputStream()
                        assertEquals(5, input.read())
                        val methods = input.read()
                        input.readNBytes(methods)
                        output.write(byteArrayOf(5, 0))
                        val header = input.readNBytes(4)
                        assertEquals(5, header[0].toInt())
                        val length = when (header[3].toInt()) {
                            1 -> 4
                            3 -> input.read()
                            4 -> 16
                            else -> error("Unexpected SOCKS address type")
                        }
                        input.readNBytes(length + 2)
                        output.write(byteArrayOf(5, 0, 0, 1, 127, 0, 0, 1, 0, 0))
                        val reader = input.bufferedReader()
                        while (!reader.readLine().isNullOrEmpty()) Unit
                        val response = "HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nOK"
                        output.write(response.toByteArray())
                    }
                }
            }
            DownloadProxy.choice = DownloadProxy.Choice.Custom(
                DownloadProxy.parseAddress("127.0.0.1:${server.localPort}"), Proxy.Type.SOCKS
            )
            try {
                val client = OkHttpClient.Builder().proxySelector(DownloadProxy.selector()).build()
                client.newCall(Request.Builder().url("http://192.0.2.1/test").build()).execute().use {
                    assertEquals("OK", it.body.string())
                }
                DownloadProxy.withJavaDownloadProxy {
                    val connection = URI("http://192.0.2.1/test").toURL().openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    assertEquals("OK", connection.getInputStream().bufferedReader().use { it.readText() })
                }
                received.get(5, TimeUnit.SECONDS)
                assertSame(original, ProxySelector.getDefault())
            } finally {
                DownloadProxy.choice = DownloadProxy.Choice.Direct
            }
        }
    }

    @Test
    fun `online requests use the chosen proxy only during initialization`() {
        val uri = URI("https://example.invalid/")
        val selector = DownloadProxy.initializationSelector()
        DownloadProxy.choice = DownloadProxy.Choice.Custom(InetSocketAddress.createUnresolved("proxy.example", 8080))
        try {
            assertEquals(Proxy.Type.DIRECT, selector.select(uri).single().type())
            DownloadProxy.onlineInitialization = true
            assertEquals(Proxy.Type.HTTP, selector.select(uri).single().type())
            assertEquals(Proxy.Type.DIRECT, selector.select(URI("http://localhost/")).single().type())
        } finally {
            DownloadProxy.onlineInitialization = false
            DownloadProxy.choice = DownloadProxy.Choice.Direct
        }
        assertEquals(Proxy.Type.DIRECT, selector.select(uri).single().type())
    }
}
