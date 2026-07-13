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

package net.ccbluex.liquidbounce.utils.text

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TextExtensionsKtTest {
    @Test
    fun testHideSensitiveAddress() {
        // Should redact subdomains
        Assertions.assertEquals("<redacted>.liquidbounce.net", "test.liquidbounce.net".hideSensitiveAddress())
        Assertions.assertEquals(
            "<redacted>.liquidbounce.net:12345",
            "test.liquidbounce.net:12345".hideSensitiveAddress()
        )
        Assertions.assertEquals("<redacted>.liquidbounce.net", "another.test.liquidbounce.net".hideSensitiveAddress())
        Assertions.assertEquals(
            "<redacted>.liquidbounce.net:54321",
            "another.test.liquidbounce.net:54321".hideSensitiveAddress()
        )
        Assertions.assertEquals("<redacted>.liquidproxy.net", "test.liquidproxy.net".hideSensitiveAddress())
        Assertions.assertEquals("<redacted>.liquidproxy.net:12345", "test.liquidproxy.net:12345".hideSensitiveAddress())

        // Should not change other addresses
        Assertions.assertEquals("example.com", "example.com".hideSensitiveAddress())
        Assertions.assertEquals("example.com:12345", "example.com:12345".hideSensitiveAddress())
        Assertions.assertEquals("localhost", "localhost".hideSensitiveAddress())
        Assertions.assertEquals("localhost:25565", "localhost:25565".hideSensitiveAddress())
        Assertions.assertEquals("liquidbounce.net", "liquidbounce.net".hideSensitiveAddress())
        Assertions.assertEquals("liquidproxy.net", "liquidproxy.net".hideSensitiveAddress())

        // Edge cases
        Assertions.assertEquals("<redacted>.liquidbounce.net", ".liquidbounce.net".hideSensitiveAddress())
        Assertions.assertEquals("<redacted>.liquidproxy.net", ".liquidproxy.net".hideSensitiveAddress())
        Assertions.assertEquals("", "".hideSensitiveAddress())
        Assertions.assertEquals(":12345", ":12345".hideSensitiveAddress())
        Assertions.assertEquals("<redacted>.liquidbounce.net:", "test.liquidbounce.net:".hideSensitiveAddress())
    }
}
