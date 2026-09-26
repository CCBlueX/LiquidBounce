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

import net.ccbluex.liquidbounce.config.gson.util.readJson
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import com.google.gson.JsonObject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The wire format has to stay compatible with what versions before the `mc-authlib` move wrote.
 */
class MinecraftAccountSerializationTest {

    @BeforeTest
    fun bootstrap() = MinecraftBootstrap.ensureInitialized()

    private fun parse(json: String): JsonObject = json.readJson()

    @Test
    fun `reads a cracked account written by an older version`() {
        val account = MinecraftAccount.fromJson(
            parse(
                """
                {
                  "name": "Player",
                  "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
                  "online": true,
                  "type": "CrackedAccount",
                  "favorite": true,
                  "bans": {}
                }
                """
            )
        )

        assertTrue(account is CrackedAccount)
        assertEquals(AccountService.CRACKED, account.service)
        assertEquals("Player", account.username)
        assertEquals("069a79f4-44e9-4726-a5be-fca90e38aaf5", account.profile?.id?.toString())
        assertTrue(account.favorite)
    }

    @Test
    fun `reads an undashed uuid`() {
        val account = MinecraftAccount.fromJson(
            parse("""{"name": "Player", "uuid": "069a79f444e94726a5befca90e38aaf5", "type": "CrackedAccount"}""")
        )

        assertEquals("069a79f4-44e9-4726-a5be-fca90e38aaf5", account.profile?.id?.toString())
    }

    @Test
    fun `keeps the account name when no uuid was saved yet`() {
        val account = MinecraftAccount.fromJson(parse("""{"name": "Player", "type": "CrackedAccount"}"""))

        assertEquals("Player", account.username)
        assertNull(account.profile)
    }

    @Test
    fun `round-trips a session account`() {
        val json = parse(
            """
            {
              "name": "Player",
              "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
              "accessToken": "token",
              "type": "SessionAccount",
              "favorite": false,
              "bans": {}
            }
            """
        )

        assertEquals(json, MinecraftAccount.fromJson(json).toJson())
    }

    @Test
    fun `round-trips an altening account`() {
        val json = parse(
            """
            {
              "name": "Player",
              "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
              "token": "token",
              "hypixelLevel": 42,
              "hypixelRank": "MVP+",
              "type": "AlteningAccount",
              "favorite": false,
              "bans": {}
            }
            """
        )

        assertEquals(json, MinecraftAccount.fromJson(json).toJson())
    }

    @Test
    fun `round-trips tracked bans`() {
        val json = parse(
            """
            {
              "name": "Player",
              "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
              "online": false,
              "type": "CrackedAccount",
              "favorite": false,
              "bans": {
                "hypixel.net": {
                  "serverName": "hypixel.net",
                  "reason": "Cheating",
                  "bannedUntil": -1
                }
              }
            }
            """
        )

        val account = MinecraftAccount.fromJson(json)

        assertContentEquals(listOf(Ban("hypixel.net", "Cheating")), account.listActiveBans())
        assertTrue(account.isBanned("hypixel.net"))
        assertEquals(json, account.toJson())
    }

    @Test
    fun `drops expired bans`() {
        val account = MinecraftAccount.fromJson(parse("""{"name": "Player", "type": "CrackedAccount"}"""))
        account.trackBan(Ban("hypixel.net", "Cheating", bannedUntil = 1L))

        assertContentEquals(emptyList(), account.listActiveBans())
    }

    @Test
    fun `rejects an unknown account type`() {
        assertFailsWith<IllegalArgumentException> {
            MinecraftAccount.fromJson(parse("""{"name": "Player", "type": "EasyMCAccount"}"""))
        }
    }

    @Test
    fun `rejects a microsoft account saved before the MinecraftAuth migration`() {
        assertFailsWith<IllegalArgumentException> {
            MinecraftAccount.fromJson(
                parse("""{"name": "Player", "refreshToken": "token", "type": "MicrosoftAccount"}""")
            )
        }
    }

}
