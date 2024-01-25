package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.api.ClientApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.command.commands.client.CommandConfig
import net.ccbluex.liquidbounce.features.command.commands.client.CommandConfig.cachedSettingsList
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.*

object ModuleAutoConfig : Module("AutoConfig", Category.MISC, state = true) {

    val blacklistedServer by textArray("Blacklist", mutableListOf(
        // Common anticheat test server
        "poke.sexy",
        "loyisa.cn",
        "anticheat-test.com"
    ))

    override fun enable() {
        val currentServerEntry = mc.currentServerEntry

        if (currentServerEntry == null) {
            notification("AutoConfig", "You are not connected to a server.",
                NotificationEvent.Severity.ERROR)
            return
        }

        loadServerConfig(currentServerEntry.address.dropPort().rootDomain())
        super.enable()
    }

    val handleServerConnect = sequenceHandler<ServerConnectEvent> {
        // Waits until the player is in game
        waitUntil { inGame && mc.currentScreen == null }

        // Loads the server config
        loadServerConfig(it.serverAddress.dropPort().rootDomain())
    }

    /**
     * Loads the config for the given server address
     */
    private fun loadServerConfig(address: String) {
        if (blacklistedServer.any { address.endsWith(it, true) }) {
            notification("Auto Config", "This server is blacklisted.",
                NotificationEvent.Severity.INFO)
            return
        }

        val autoConfig = cachedSettingsList?.find {
            it.serverAddress?.rootDomain().equals(address, true) ||
                it.serverAddress.equals(address, true)
        }

        if (autoConfig == null) {
            notification("Auto Config", "There is no known config for $address.",
                NotificationEvent.Severity.ERROR)
            return
        }

        CommandConfig.loadingNow = true
        runCatching {
            ClientApi.requestSettingsScript(autoConfig.settingId).apply {
                ConfigSystem.deserializeConfigurable(
                    ModuleManager.modulesConfigurable, reader(),
                    ConfigSystem.autoConfigGson)
            }

        }.onFailure {
            notification("Auto Config", "Failed to load config ${autoConfig.name}.",
                NotificationEvent.Severity.ERROR)
        }.onSuccess {
            notification("Auto Config", "Successfully loaded config ${autoConfig.name}.",
                NotificationEvent.Severity.SUCCESS)
        }
        CommandConfig.loadingNow = false
    }

    /**
     * Overwrites the condition requirement for being in game
     */
    override fun handleEvents() = enabled

}
