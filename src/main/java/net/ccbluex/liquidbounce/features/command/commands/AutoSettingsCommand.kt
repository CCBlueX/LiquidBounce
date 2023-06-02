/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.api.ClientApi
import net.ccbluex.liquidbounce.api.autoSettingsList
import net.ccbluex.liquidbounce.api.loadSettings
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.SettingsUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.get
import kotlin.concurrent.thread

class AutoSettingsCommand : Command("autosettings", "setting", "settings", "config", "autosetting") {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size <= 1) {
            chatSyntax("settings <load/list>")

            return
        }

        when {
            // Load subcommand
            args[1].equals("load", ignoreCase = true) -> {
                if (args.size < 3) {
                    chatSyntax("settings load <name/url>")
                    return
                }

                thread {
                    runCatching {
                        chat("Loading settings...")

                        // Load settings and apply them
                        val settings = if (args[2].startsWith("http")) {
                            get(args[2])
                        } else {
                            ClientApi.requestSettingsScript(args[2])
                        }

                        chat("Applying settings...")
                        SettingsUtils.applyScript(settings)
                    }.onSuccess {
                        chat("§6Settings applied successfully")
                        addNotification(Notification("Updated Settings"))
                        playEdit()
                    }.onFailure {
                        LOGGER.error("Failed to load settings", it)
                        chat("Failed to load settings: ${it.message}")
                    }
                }
            }

            // List subcommand
            args[1].equals("list", ignoreCase = true) -> {
                chat("Loading settings...")

                loadSettings(false) {
                    for (setting in it) {
                        chat("> ${setting.settingId} (Last updated: ${setting.date}, Status: ${setting.statusType.displayName})")
                    }
                }
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) {
            return emptyList()
        }

        return when (args.size) {
            1 -> listOf("list", "load").filter { it.startsWith(args[0], true) }
            2 -> {
                if (args[0].equals("load", true)) {
                    if (autoSettingsList == null) {
                        loadSettings(true, 500) {}
                    }

                    if (autoSettingsList != null) {
                        return autoSettingsList!!.filter { it.settingId.startsWith(args[1], true) }.map { it.settingId }
                    }
                }
                return emptyList()
            }
            else -> emptyList()
        }
    }
}