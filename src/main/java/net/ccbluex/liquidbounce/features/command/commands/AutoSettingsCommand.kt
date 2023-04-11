/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.api.autoSettingFiles
import net.ccbluex.liquidbounce.api.loadSettings
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
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

                // Settings url
                val url = if (args[2].startsWith("http")) {
                    args[2]
                } else {
                    "${CLIENT_CLOUD}/settings/${args[2].lowercase()}"
                }

                chat("Loading settings...")

                thread {
                    try {
                        // Load settings and apply them
                        val settings = get(url)

                        chat("Applying settings...")
                        SettingsUtils.executeScript(settings)
                        chat("§6Settings applied successfully")
                        addNotification(Notification("Updated Settings"))
                        playEdit()
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                        chat("Failed to fetch auto settings.")
                    }
                }
            }

            // List subcommand
            args[1].equals("list", ignoreCase = true) -> {
                chat("Loading settings...")

                loadSettings(false) {
                    for (setting in it)
                        chat("> ${setting.name} (last updated: ${setting.lastModified})")
                }
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("list", "load").filter { it.startsWith(args[0], true) }
            2 -> {
                if (args[0].equals("load", true)) {
                    if (autoSettingFiles == null) {
                        loadSettings(true, 500) {}
                    }

                    if (autoSettingFiles != null) {
                        return autoSettingFiles!!.filter { it.name.startsWith(args[1], true) }.map { it.name }
                    }
                }
                return emptyList()
            }
            else -> emptyList()
        }
    }
}