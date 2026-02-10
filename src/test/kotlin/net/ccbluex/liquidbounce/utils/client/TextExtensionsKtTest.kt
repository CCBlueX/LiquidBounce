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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextExtensionsKtTest {
    @Test
    fun testHideSensitiveAddress() {
        // Should redact subdomains
        assertEquals("<redacted>.liquidbounce.net", "test.liquidbounce.net".hideSensitiveAddress())
        assertEquals("<redacted>.liquidbounce.net:12345", "test.liquidbounce.net:12345".hideSensitiveAddress())
        assertEquals("<redacted>.liquidbounce.net", "another.test.liquidbounce.net".hideSensitiveAddress())
        assertEquals("<redacted>.liquidbounce.net:54321", "another.test.liquidbounce.net:54321".hideSensitiveAddress())
        assertEquals("<redacted>.liquidproxy.net", "test.liquidproxy.net".hideSensitiveAddress())
        assertEquals("<redacted>.liquidproxy.net:12345", "test.liquidproxy.net:12345".hideSensitiveAddress())

        // Should not change other addresses
        assertEquals("example.com", "example.com".hideSensitiveAddress())
        assertEquals("example.com:12345", "example.com:12345".hideSensitiveAddress())
        assertEquals("localhost", "localhost".hideSensitiveAddress())
        assertEquals("localhost:25565", "localhost:25565".hideSensitiveAddress())
        assertEquals("liquidbounce.net", "liquidbounce.net".hideSensitiveAddress())
        assertEquals("liquidproxy.net", "liquidproxy.net".hideSensitiveAddress())

        // Edge cases
        assertEquals("<redacted>.liquidbounce.net", ".liquidbounce.net".hideSensitiveAddress())
        assertEquals("<redacted>.liquidproxy.net", ".liquidproxy.net".hideSensitiveAddress())
        assertEquals("", "".hideSensitiveAddress())
        assertEquals(":12345", ":12345".hideSensitiveAddress())
        assertEquals("<redacted>.liquidbounce.net:", "test.liquidbounce.net:".hideSensitiveAddress())
    }
}
