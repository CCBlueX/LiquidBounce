/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.AutoConfig.configs
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.dropPort
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.rootDomain

object ModuleAutoConfig : Module("AutoConfig", Category.CLIENT, state = true, aliases = arrayOf("AutoSettings")) {

    private val blacklistedServer = mutableListOf(
        // Common anticheat test server
        "anticheat-test.com"
    )
    private var requiresConfigLoad = false

    init {
        doNotInclude()
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

        loadServerConfig(currentServerEntry.address.dropPort().rootDomain())
        super.enable()
    }

    override fun disable() {
        requiresConfigLoad = false
        super.disable()
    }

    val repeatable = repeatable {
        if (requiresConfigLoad && inGame && mc.currentScreen == null) {
            enable()
            requiresConfigLoad = false
        }
    }

    @Suppress("unused")
    val handleServerConnect = handler<ServerConnectEvent> {
        requiresConfigLoad = true
    }

    /**
     * Loads the config for the given server address
     */
    private fun loadServerConfig(address: String) {
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
        val autoConfig = configs.filter {
            it.serverAddress?.rootDomain().equals(address, true) ||
                    it.serverAddress.equals(address, true)
        }.minByOrNull { it.name.length }

        if (autoConfig == null) {
            notification(
                "Auto Config", "There is no known config for $address.",
                NotificationEvent.Severity.ERROR
            )
            return
        }

        AutoConfig.loadAutoConfig(autoConfig)
    }

    /**
     * Overwrites the condition requirement for being in game
     */
    override fun handleEvents() = enabled

}
