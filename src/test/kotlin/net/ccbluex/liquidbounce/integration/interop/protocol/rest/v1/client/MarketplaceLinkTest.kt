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
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarketplaceLinkTest {

    private val description = """
        # Example
        See the [wiki](https://example.org/wiki/Setup), <https://example.org/a?b=1&c=2>
        or https://example.com.au/download.
    """.trimIndent()

    @Test
    fun `links of the description are found`() {
        assertTrue(hasLink(description, "https://example.org/wiki/Setup"))
        assertTrue(hasLink(description, "https://example.org/a?b=1&c=2"))
        assertTrue(hasLink(description, "https://example.com.au/download"))
    }

    @Test
    fun `other urls are not`() {
        assertFalse(hasLink(description, "https://example.net/"))
        assertFalse(hasLink(description, "file:///etc/passwd"))
        assertFalse(hasLink(description, ""))
    }

    @Test
    fun `a part of a link is not a link`() {
        assertFalse(hasLink(description, "https://example.org/wiki"))
        assertFalse(hasLink(description, "https://example.com"))
        assertFalse(hasLink(description, "example.org/wiki/Setup"))
    }
}
