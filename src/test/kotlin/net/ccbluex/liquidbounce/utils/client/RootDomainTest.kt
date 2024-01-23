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
