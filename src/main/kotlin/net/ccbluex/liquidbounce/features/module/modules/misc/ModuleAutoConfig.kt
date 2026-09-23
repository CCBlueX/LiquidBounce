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

package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.event.eventListenerScope
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.ConfigTracker
import net.ccbluex.liquidbounce.features.marketplace.autoconfig.MarketplaceConfigs
import net.ccbluex.liquidbounce.features.misc.SelfDestruct
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.text.dropPort
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.text.rootDomain
import net.minecraft.client.gui.screens.ConnectScreen

object ModuleAutoConfig : ClientModule(
    "AutoConfig",
    ModuleCategories.MISC,
    state = true,
    aliases = listOf("AutoSettings")
) {

    private val blacklistedServer = mutableListOf(
        // Common anticheat test server
        "poke.sexy",
        "loyisa.cn",
        "anticheat-test.com"
    )
    @Volatile
    private var isScheduled = false

    init {
        doNotIncludeAlways()
    }

    override suspend fun enabledEffect() {
        val currentServerEntry = mc.currentServer

        if (currentServerEntry == null) {
            notification(
                "AutoConfig", "You are not connected to a server.",
                NotificationEvent.Severity.ERROR
            )
            return
        }

        loadServerConfig(currentServerEntry.ip.dropPort().rootDomain(), null)
    }

    @Suppress("unused")
    private val handleServerConnect = handler<ServerConnectEvent> { event ->
        if (isScheduled) {
            return@handler
        }

        // This will stop us from connecting to the server right away
        event.cancelEvent()

        eventListenerScope.launch {
            try {
                isScheduled = true
                val address = event.serverInfo.ip.dropPort().rootDomain()

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

        val autoConfig = runCatching { MarketplaceConfigs.findForServer(address) }
            .onFailure { logger.error("Failed to look up a config for $address.", it) }
            .getOrNull()
        val revisionId = autoConfig?.liveRevisionId

        if (autoConfig == null || revisionId == null) {
            notification(
                "Auto Config", "There is no known config for $address.",
                NotificationEvent.Severity.ERROR
            )
            return
        }

        // Loading again would throw away the user's edits or re-apply what already runs
        if (ConfigTracker.state != ConfigTracker.State.NONE && ConfigTracker.itemId == autoConfig.id &&
            (ConfigTracker.state == ConfigTracker.State.EDITING || ConfigTracker.revisionId == revisionId)
        ) {
            return
        }

        connectScreen?.updateStatus(regular(message("loading", address)))
        runCatching {
            ConfigTracker.load(autoConfig, revisionId)
        }.onFailure { error ->
            logger.error("Failed to load config ${autoConfig.name} for $address.", error)
            connectScreen?.updateStatus(markAsError(message("failed", address)))
            notification(
                "Auto Config", "Failed to load config ${autoConfig.name}.",
                NotificationEvent.Severity.ERROR
            )
        }.onSuccess {
            connectScreen?.updateStatus(regular(message("loaded", address)))
            notification(
                "Auto Config", "Successfully loaded config ${autoConfig.name}.",
                NotificationEvent.Severity.SUCCESS
            )
        }
    }

    /**
     * Overwrites the condition requirement for being in-game
     */
    override val running
        get() = !SelfDestruct.isDestructed && enabled

}
