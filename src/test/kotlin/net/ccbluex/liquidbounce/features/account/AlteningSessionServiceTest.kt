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

package net.ccbluex.liquidbounce.features.account

import com.google.gson.JsonParser
import com.mojang.authlib.exceptions.AuthenticationUnavailableException
import com.mojang.authlib.exceptions.InvalidCredentialsException
import com.mojang.authlib.services.MinecraftServicesDiscoveryService
import com.mojang.authlib.services.MinecraftServicesSessionService
import com.mojang.authlib.services.ProfileActionType
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AlteningSessionServiceTest {

    private data class Request(val method: String, val uri: String, val body: String, val authorization: String?)

    private val profileId = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5")
    private val undashedId = profileId.toString().replace("-", "")
    private val requests = LinkedBlockingQueue<Request>()
    private lateinit var server: HttpServer
    private lateinit var service: AlteningSessionService
    private var responseCode = 204
    private var responseBody = ""

    @BeforeTest
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.add(Request(
                exchange.requestMethod,
                exchange.requestURI.toString(),
                exchange.requestBody.bufferedReader().use { it.readText() },
                exchange.requestHeaders.getFirst("Authorization"),
            ))
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(responseCode, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        server.start()
        service = AlteningSessionService(
            MinecraftServicesDiscoveryService.createOffline(Proxy.NO_PROXY),
            URI.create("http://127.0.0.1:${server.address.port}"),
        )
    }

    @AfterTest
    fun stopServer() = server.stop(0)

    @Test
    fun `join uses the legacy endpoint without fetching a discovery document`() {
        service.joinServer(profileId, "test-token", "server-hash")

        val request = requests.remove()
        assertEquals("POST", request.method)
        assertEquals("/session/minecraft/join", request.uri)
        assertNull(request.authorization)
        val body = JsonParser.parseString(request.body).asJsonObject
        assertEquals("test-token", body["accessToken"].asString)
        assertEquals(undashedId, body["selectedProfile"].asString)
        assertEquals("server-hash", body["serverId"].asString)
        assertEquals(0, requests.size)
    }

    @Test
    fun `invalid token becomes an authentication error`() {
        responseCode = 403
        responseBody = """{"error":"ForbiddenOperationException","errorMessage":"Invalid token"}"""

        assertFailsWith<InvalidCredentialsException> { service.joinServer(profileId, "invalid", "hash") }
        assertEquals("/session/minecraft/join", requests.remove().uri)
        assertEquals(0, requests.size)
    }

    @Test
    fun `unavailable provider becomes an authentication error`() {
        responseCode = 503

        assertFailsWith<AuthenticationUnavailableException> { service.joinServer(profileId, "token", "hash") }
    }

    @Test
    fun `verify decodes the legacy profile response and sends the remote address`() {
        responseCode = 200
        responseBody = """
            {"id":"$undashedId","properties":[{"name":"textures","value":"skin","signature":"signed"}],
             "profileActions":[{"updateType":"FORCED_NAME_CHANGE"}]}
        """.trimIndent()

        val result = assertNotNull(service.hasJoinedServer("Player", "hash", InetAddress.getByName("127.0.0.2")))
        assertEquals(profileId, result.profile().id())
        assertEquals("Player", result.profile().name())
        assertEquals("skin", result.profile().properties().get("textures").single().value())
        assertEquals(setOf(ProfileActionType.FORCED_NAME_CHANGE), result.actions())
        val request = requests.remove()
        assertEquals("GET", request.method)
        assertEquals("/session/minecraft/hasJoined", request.uri.substringBefore('?'))
        val arguments = request.uri.substringAfter('?').split('&').toSet()
        assertEquals(setOf("username=Player", "serverId=hash", "ip=127.0.0.2"), arguments)
    }

    @Test
    fun `profile lookup preserves signed properties and the unsigned flag`() {
        responseCode = 200
        responseBody = """
            {"id":"$undashedId","name":"Player",
             "properties":[{"name":"textures","value":"skin","signature":"signed"}]}
        """.trimIndent()

        for (requireSecure in listOf(true, false)) {
            val result = assertNotNull(service.fetchProfile(profileId, requireSecure))
            assertEquals("signed", result.profile().properties().get("textures").single().signature())
            assertEquals("/session/minecraft/profile/$undashedId?unsigned=${!requireSecure}", requests.remove().uri)
        }
    }

    @Test
    fun `missing profiles and unverified sessions return null`() {
        assertNull(service.fetchProfile(profileId, true))
        assertNull(service.hasJoinedServer("Player", "hash", null))
    }

    @Test
    fun `only Altening accounts replace the Mojang session implementation`() {
        val discovery = MinecraftServicesDiscoveryService.createOffline(Proxy.NO_PROXY)
        assertIs<AlteningSessionService>(AlteningAccount().createMinecraftSessionService(discovery))
        assertEquals(
            MinecraftServicesSessionService::class.java.name,
            SessionAccount("test-token").createMinecraftSessionService(discovery).javaClass.name,
        )
    }

}
