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
package net.ccbluex.liquidbounce.gametest

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Config
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

class ConfigPersistenceGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.onClient {
            val config = Config("GameTestPersistence")
            val value = config.int("Number", 1, 0..10)
            ConfigSystem.root(config)
            try {
                ConfigSystem.store(config)
                check(config.jsonFile.isFile) { "First save did not create a config" }

                value.set(2)
                ConfigSystem.store(config)
                val saved = config.jsonFile.bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
                check(saved["value"].asJsonArray[0].asJsonObject["value"].asInt == 2) {
                    "Saving did not replace the existing config"
                }
                check(!config.jsonTmpFile.exists()) { "A successful save left its temporary file behind" }

                val previous = config.jsonFile.readBytes()
                check(config.jsonTmpFile.mkdir()) { "Could not block the temporary write" }
                value.set(3)
                ConfigSystem.store(config)
                check(previous.contentEquals(config.jsonFile.readBytes())) {
                    "A failed temporary write changed the saved config"
                }

                check(config.jsonTmpFile.delete()) { "Could not unblock the temporary write" }
                ConfigSystem.store(config)
                value.set(0)
                ConfigSystem.load(config)
                check(value.get() == 3) { "Saving did not recover after a failed temporary write" }
            } finally {
                config.jsonTmpFile.deleteRecursively()
                config.jsonFile.delete()
                ConfigSystem.remove(config)
            }
        }
    }

}
