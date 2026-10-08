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

package net.ccbluex.liquidbounce.features.chat.packet

import com.google.gson.GsonBuilder
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AxochatPacketGsonTest {

    private val serializer = GsonBuilder()
        .registerTypeAdapter(AxochatPacket.C2S::class.java, PacketSerializer().apply {
            register<C2SHelloPacket>("Hello")
            register<C2SRequestMojangInfoPacket>("RequestMojangInfo")
            register<C2SPartyPacket>("Party")
        })
        .create()

    private val deserializer = GsonBuilder()
        .registerTypeAdapter(AxochatPacket.S2C::class.java, PacketDeserializer().apply {
            register<S2CErrorPacket>("Error")
            register<S2CPartyPacket>("Party")
        })
        .create()

    @BeforeTest
    fun bootstrapMinecraft() {
        MinecraftBootstrap.ensureInitialized()
    }

    private fun encode(packet: AxochatPacket.C2S) = serializer.toJson(packet, AxochatPacket.C2S::class.java)

    private fun decode(json: String) = deserializer.fromJson(json, AxochatPacket.S2C::class.java)

    @Test
    fun `packets without body have no content`() {
        assertEquals("""{"m":"RequestMojangInfo"}""", encode(C2SRequestMojangInfoPacket()))
        assertEquals("""{"m":"Hello","c":{"protocol":2}}""", encode(C2SHelloPacket(2)))
    }

    @Test
    fun `absent optional fields are left out`() {
        assertEquals("""{"m":"Party","c":{"action":"pvp","enabled":false}}""",
            encode(C2SPartyPacket("pvp", enabled = false)))
    }

    @Test
    fun `unknown and malformed packets decode to null`() {
        assertNull(decode("""{"m":"SomethingNew","c":{"x":1}}"""))
        assertNull(decode("""{"c":{}}"""))
        assertNull(decode("""[]"""))
    }

    @Test
    fun `error codes read in both shapes`() {
        val modern = decode("""{"m":"Error","c":{"message":"InvalidCharacter","detail":"x"}}""")
        assertIs<S2CErrorPacket>(modern)
        assertEquals("InvalidCharacter", modern.code)
        assertEquals("x", modern.details)

        val legacy = decode("""{"m":"Error","c":{"message":{"InvalidCharacter":"y"}}}""")
        assertIs<S2CErrorPacket>(legacy)
        assertEquals("InvalidCharacter", legacy.code)
        assertEquals("y", legacy.details)
    }

    @Test
    fun `party may be null`() {
        val packet = assertIs<S2CPartyPacket>(decode("""{"m":"Party","c":{"party":null}}"""))
        assertNull(packet.party)
    }

}
