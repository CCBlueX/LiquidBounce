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
package net.ccbluex.liquidbounce.integration.task

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.utils.client.logger
import java.net.Proxy
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/** Stores only the proxy mode and address, never credentials. */
internal object DownloadProxyStorage {
    fun saveCurrent() {
        runCatching {
            save(ConfigSystem.rootFolder.toPath().resolve("startup-proxy.properties"), DownloadProxy.choice)
        }.onFailure { logger.warn("Failed to save startup proxy selection", it) }
    }

    fun shouldPrompt(saved: DownloadProxy.Choice?, initialized: Boolean, requested: Boolean, gameTest: Boolean) =
        requested || (!gameTest && (!initialized || saved == null))

    fun isInitialized(root: Path) = Files.exists(root.resolve("startup-initialized"))

    fun markInitialized() {
        runCatching {
            Files.writeString(ConfigSystem.rootFolder.toPath().resolve("startup-initialized"), "initialized\n")
        }.onFailure { logger.warn("Failed to record completed initialization", it) }
    }

    fun load(path: Path): DownloadProxy.Choice? = runCatching {
        val properties = Properties().apply { Files.newInputStream(path).use(::load) }
        when (properties.getProperty("mode")) {
            "direct" -> DownloadProxy.Choice.Direct
            "system" -> DownloadProxy.Choice.System
            "custom" -> DownloadProxy.Choice.Custom(
                DownloadProxy.parseAddress(properties.getProperty("address")),
                when (properties.getProperty("type", "http")) {
                    "http" -> Proxy.Type.HTTP
                    "socks" -> Proxy.Type.SOCKS
                    else -> error("Unknown proxy type")
                }
            )
            else -> null
        }
    }.getOrNull()

    fun save(path: Path, choice: DownloadProxy.Choice) {
        val properties = Properties()
        when (choice) {
            DownloadProxy.Choice.Direct -> properties.setProperty("mode", "direct")
            DownloadProxy.Choice.System -> properties.setProperty("mode", "system")
            is DownloadProxy.Choice.Custom -> {
                properties.setProperty("mode", "custom")
                properties.setProperty("type", if (choice.type == Proxy.Type.SOCKS) "socks" else "http")
                val host = choice.address.hostString
                properties.setProperty("address", "${if (':' in host) "[$host]" else host}:${choice.address.port}")
            }
        }
        Files.createDirectories(path.parent)
        Files.newOutputStream(path).use { properties.store(it, "LiquidBounce startup download proxy") }
    }
}
