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
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class ConfigPersistenceGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.onClient {
            val config = Config("GameTestPersistence")
            val value = config.int("Number", 1, 0..10)
            ConfigSystem.root(config)
            val target = config.jsonFile.toPath()
            try {
                ConfigSystem.store(config)
                check(config.jsonFile.isFile) { "First save did not create a config" }

                value.set(2)
                ConfigSystem.store(config)
                val saved = config.jsonFile.bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
                check(saved["value"].asJsonArray[0].asJsonObject["value"].asInt == 2) {
                    "Saving did not replace the existing config"
                }
                check(leftoverTemps(target).isEmpty()) { "A successful save left its temporary file behind" }

                // A non-empty directory cannot be replaced, so the save has to fail and keep it.
                check(config.jsonFile.delete()) { "Could not block the config file" }
                target.createDirectory().resolve("blocker").createFile()
                value.set(3)
                ConfigSystem.store(config)
                check(target.isDirectory()) { "A failed save replaced the config file" }
                check(leftoverTemps(target).isEmpty()) { "A failed save left its temporary file behind" }

                config.jsonFile.deleteRecursively()
                ConfigSystem.store(config)
                value.set(0)
                ConfigSystem.load(config)
                check(value.get() == 3) { "Saving did not recover after a failed save" }
            } finally {
                leftoverTemps(target).forEach { it.deleteIfExists() }
                config.jsonFile.deleteRecursively()
                ConfigSystem.remove(config)
            }
        }
    }

    /** Temporary siblings that only a failed save leaves behind. */
    private fun leftoverTemps(target: Path): List<Path> =
        Files.newDirectoryStream(target.parent, "${target.name}.*.tmp").use { it.toList() }

}
