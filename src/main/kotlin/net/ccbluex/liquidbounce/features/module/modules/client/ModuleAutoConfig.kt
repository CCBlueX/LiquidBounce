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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.AutoConfig.configs
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen

object ModuleAutoConfig : ClientModule("AutoConfig", Category.CLIENT, state = true, aliases = arrayOf("AutoSettings")) {

    private val blacklistedServer = mutableListOf(
        // Common anticheat test server
        "poke.sexy",
        "loyisa.cn",
        "anticheat-test.com"
    )
    private var isScheduled = false

    init {
        doNotIncludeAlways()
    }

    override fun enable() {
        val currentServerEntry = mc.currentServerEntry

        if (currentServerEntry == null) {
            notification(
                "AutoConfig", "You are not connected to a server.",
                NotificationEvent.Severity.ERROR
            )
            return
        }

        withScope {
            loadServerConfig(currentServerEntry.address.dropPort().rootDomain(), null)
        }
        super.enable()
    }

    @Suppress("unused")
    private val handleServerConnect = handler<ServerConnectEvent> { event ->
        if (isScheduled) {
            return@handler
        }

        // This will stop us from connecting to the server right away
        event.cancelEvent()

        withScope {
            try {
                isScheduled = true
                val address = event.serverInfo.address.dropPort().rootDomain()

                loadServerConfig(address, event.connectScreen)
            } finally {
                // Proceed to connect to the server
                event.connectScreen.connect(mc, event.address, event.serverInfo, event.cookieStorage)
                isScheduled = false
            }
        }
    }

    /**
     * Loads the config for the given server address
     */
    private suspend fun loadServerConfig(
        address: String,
        connectScreen: ConnectScreen? = null
    ) {
        if (blacklistedServer.any { address.endsWith(it, true) }) {
            notification(
                "Auto Config", "This server is blacklisted.",
                NotificationEvent.Severity.INFO
            )
            return
        }

        // Get config with the shortest name, as it is most likely the correct one.
        // There can be multiple configs for the same server, but with different names
        // and the global config is likely named e.g "hypixel", while the more specific ones are named
        // "hypixel-csgo", "hypixel-legit", etc.
        val autoConfig = (configs ?: return).filter { config ->
            config.serverAddress?.rootDomain().equals(address, true) ||
                    config.serverAddress.equals(address, true)
        }.minByOrNull { config -> config.name.length }

        if (autoConfig == null) {
            notification(
                "Auto Config", "There is no known config for $address.",
                NotificationEvent.Severity.ERROR
            )
            return
        }

        connectScreen?.setStatus(regular(message("loading", address)))
        runCatching {
            AutoConfig.loadAutoConfig(autoConfig)
        }.onFailure { error ->
            logger.error("Failed to load config ${autoConfig.name} for $address.", error)
            connectScreen?.setStatus(markAsError(message("failed", address)))
            notification("Auto Config", "Failed to load config ${autoConfig.name}.",
                NotificationEvent.Severity.ERROR)
        }.onSuccess {
            connectScreen?.setStatus(regular(message("loaded", address)))
            notification("Auto Config", "Successfully loaded config ${autoConfig.name}.",
                NotificationEvent.Severity.SUCCESS)
        }
    }

    /**
     * Overwrites the condition requirement for being in-game
     */
    override val running
        get() = !isDestructed && enabled

}
