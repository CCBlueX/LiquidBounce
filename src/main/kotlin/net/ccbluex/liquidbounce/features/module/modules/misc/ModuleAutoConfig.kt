package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.api.ClientApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.command.commands.client.CommandConfig.cachedSettingsList
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.*

object ModuleAutoConfig : Module("AutoConfig", Category.MISC) {

    val blacklistedServer by textArray("Blacklist", mutableListOf(
        // Common anticheat test server
        "poke.sexy",
        "eu.loyisa.cn",
        "anticheat-test.com"
    ))

    override fun enable() {
        val currentServerEntry = mc.currentServerEntry

        if (currentServerEntry == null) {
            notification("AutoConfig", "You are not connected to a server.",
                NotificationEvent.Severity.ERROR)
            return
        }


        super.enable()
    }

    val handleServerConnect = sequenceHandler<ServerConnectEvent> {
        // Waits until the player is in game
        waitUntil { inGame && mc.currentScreen == null }

        // Loads the server config
        loadServerConfig(it.serverAddress)
    }

    /**
     * Loads the config for the given server address
     */
    private fun loadServerConfig(address: String) {
        if (blacklistedServer.any { address.contains(it, true) }) {
            notification("AutoConfig", "This server is blacklisted.",
                NotificationEvent.Severity.INFO)
            return
        }

        val autoConfig = cachedSettingsList?.find { it.serverAddress.equals(address, true) }

        if (autoConfig == null) {
            notification("AutoConfig", "No config found for this server.",
                NotificationEvent.Severity.ERROR)
            return
        }

        runCatching {
            ClientApi.requestSettingsScript(autoConfig.settingId).apply {
                ConfigSystem.deserializeConfigurable(
                    ModuleManager.modulesConfigurable, reader(),
                    ConfigSystem.autoConfigGson)
            }

        }.onFailure {
            notification("AutoConfig", "Failed to load config for this server.",
                NotificationEvent.Severity.ERROR)
        }.onSuccess {
            notification("AutoConfig", "Loaded config for this server.",
                NotificationEvent.Severity.SUCCESS)
        }
    }

    /**
     * Overwrites the condition requirement for being in game
     */
    override fun handleEvents() = enabled

}
