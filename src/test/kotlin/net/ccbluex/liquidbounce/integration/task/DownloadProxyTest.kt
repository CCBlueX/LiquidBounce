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
import java.net.Proxy
import java.net.ProxySelector
import java.net.ServerSocket
import java.net.URI
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class DownloadProxyTest {
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
                val selector = DownloadProxy.selector(original)
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
        assertEquals(original.select(uri), DownloadProxy.selector(original, DownloadProxy.Choice.System).select(uri))
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
    fun `online requests use the chosen proxy only during initialization`() {
        val uri = URI("https://example.invalid/")
        val selector = DownloadProxy.initializationSelector(ProxySelector.getDefault())
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
