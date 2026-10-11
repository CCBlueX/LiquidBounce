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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ProxyParsingTest {

    @Test
    fun `preserves hostname and IPv4 import formats`() {
        for (host in listOf("proxy.example", "127.0.0.1")) {
            assertProxy("$host:1080", host)
            assertProxy("$host:1080:alice:secret", host, "alice", "secret")
            assertProxy("alice:secret:$host:1080", host, "alice", "secret")
            assertProxy("alice:secret@$host:1080", host, "alice", "secret")
        }
    }

    @Test
    fun `accepts bracketed IPv6 in every import format`() {
        for (host in listOf("::1", "2001:db8::1", "2001:db8:1234:5678:90ab:cdef:1234:5678")) {
            assertProxy("[$host]:1080", host)
            assertProxy("[$host]:1080:alice:secret", host, "alice", "secret")
            assertProxy("alice:secret:[$host]:1080", host, "alice", "secret")
            assertProxy("alice:secret@[$host]:1080", host, "alice", "secret")
        }
    }

    @Test
    fun `keeps the requested proxy protocol with an IPv6 host`() {
        for ((scheme, type) in listOf(
            "http" to Proxy.Type.HTTP,
            "socks5" to Proxy.Type.SOCKS5,
            "socks5h" to Proxy.Type.SOCKS5,
        )) {
            assertEquals(type, Proxy.parse("$scheme://alice:secret@[::1]:1080").type)
        }
    }

    @Test
    fun `preserves colons and at signs in URI style passwords`() {
        assertProxy("alice:s:e@c:r@et@[::1]:1080", "::1", "alice", "s:e@c:r@et")
    }

    @Test
    fun `does not treat brackets in legacy passwords as host delimiters`() {
        for (password in listOf("pa[ss]word", "[secret", "secret]")) {
            assertProxy("proxy.example:1080:alice:$password", "proxy.example", "alice", password)
            assertProxy("alice:$password:proxy.example:1080", "proxy.example", "alice", password)
        }
    }

    @Test
    fun `keeps host first precedence when legacy credentials contain a numeric password`() {
        assertProxy("proxy.example:1080:alice:1234", "proxy.example", "alice", "1234")
    }

    @Test
    fun `rejects missing hosts ports credentials and malformed brackets`() {
        for (text in listOf(
            "", ":1080", "proxy.example", "proxy.example:abc",
            "[::1]", "[::1:1080", "::1:1080", "[::1]]:1080",
            "alice@proxy.example:1080", "alice:secret@[]:1080",
        )) {
            assertFailsWith<IllegalArgumentException>(text) { Proxy.parse(text) }
        }
    }

    @Test
    fun `validates port bounds for every format`() {
        for (port in listOf("0", "-1", "65536", "2147483648")) {
            for (text in listOf(
                "proxy.example:$port",
                "[::1]:$port",
                "alice:secret@[::1]:$port",
                "[::1]:$port:alice:secret",
                "alice:secret:[::1]:$port",
            )) {
                assertFailsWith<IllegalArgumentException>(text) { Proxy.parse(text) }
            }
        }
        assertEquals(1, Proxy.parse("[::1]:1").port)
        assertEquals(65535, Proxy.parse("[::1]:65535").port)
    }

    private fun assertProxy(text: String, host: String, username: String? = null, password: String? = null) {
        val proxy = Proxy.parse(text)
        assertEquals(host, proxy.host, text)
        assertEquals(1080, proxy.port, text)
        assertEquals(Proxy.Type.SOCKS5, proxy.type, text)
        if (username == null) {
            assertNull(proxy.credentials, text)
        } else {
            assertEquals(username, proxy.credentials?.username, text)
            assertEquals(password, proxy.credentials?.password, text)
        }
    }

}
