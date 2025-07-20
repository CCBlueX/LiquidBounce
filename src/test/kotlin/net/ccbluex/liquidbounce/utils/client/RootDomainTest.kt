/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

class RootDomainTest {

    @Test
    fun testRootDomain() {
        val domain = "ccbluex.net"
        assertEquals("ccbluex.net", domain.rootDomain())
    }

    @Test
    fun testRootDomainWithSubdomain() {
        val domain = "test.ccbluex.net"
        assertEquals("ccbluex.net", domain.rootDomain())
    }

    @Test
    fun testRootDomainWithDot() {
        val domain = "ccbluex.net."
        assertEquals("ccbluex.net", domain.rootDomain())
    }

    @Test
    fun testRootDomainWithSubdomainAndDot() {
        val domain = "test.ccbluex.net."
        assertEquals("ccbluex.net", domain.rootDomain())
    }

    @Test
    fun testRootDomainWithIp() {
        val domain = "127.0.0.1"

        assertEquals(domain, domain.rootDomain())
    }

    @Test
    fun testRootDomainWithIpAndPort() {
        val domain = "127.0.0.1:25565"

        assertEquals("127.0.0.1", domain.dropPort().rootDomain())
    }

}
