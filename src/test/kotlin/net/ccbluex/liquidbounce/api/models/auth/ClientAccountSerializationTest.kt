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
package net.ccbluex.liquidbounce.api.models.auth

import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.config.gson.fileGson
import net.ccbluex.liquidbounce.config.gson.util.readJson
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * LiquidLauncher reads and writes `account.json` as well, so its format has to stay.
 */
class ClientAccountSerializationTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }
    }

    private val session = OAuthSession(ExpiryValue("access", 4102444800000), "refresh")

    private val file: JsonObject = """
        {
          "name": "account",
          "value": [
            {
              "name": "account",
              "value": {
                "session": {
                  "accessToken": { "value": "access", "expiresAt": 4102444800000 },
                  "refreshToken": "refresh"
                }
              }
            }
          ]
        }
    """.readJson()

    @Test
    fun `writes the session where LiquidLauncher reads it`() {
        ClientAccountManager.clientAccount = ClientAccount(session)

        assertEquals(file, fileGson.toJsonTree(ClientAccountManager, ValueGroup::class.java))
    }

    @Test
    fun `reads the session LiquidLauncher writes`() {
        val account = file["value"].asJsonArray.single().asJsonObject["value"]
        ClientAccountManager.inner.single().deserializeFrom(fileGson, account)

        assertEquals(session, runBlocking { ClientAccountManager.clientAccount.takeSession() })
    }

}
