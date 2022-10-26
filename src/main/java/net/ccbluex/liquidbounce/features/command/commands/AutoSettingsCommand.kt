/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.SettingsUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import kotlin.concurrent.thread

class AutoSettingsCommand : Command("autosettings", "setting", "settings", "config", "autosetting") {
    private val loadingLock = Object()
    private var autoSettingFiles: MutableList<String>? = null

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
                val url = if (args[2].startsWith("http"))
                    args[2]
                else
                    "${LiquidBounce.CLIENT_CLOUD}/settings/${args[2].lowercase()}"

                chat("Loading settings...")

                thread {
                    try {
                        // Load settings and apply them
                        val settings = HttpUtils.get(url)

                        chat("Applying settings...")
                        SettingsUtils.executeScript(settings)
                        chat("§6Settings applied successfully")
                        LiquidBounce.hud.addNotification(Notification("Updated Settings"))
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
                        chat("> $setting")
                }
            }
        }
    }

    private fun loadSettings(useCached: Boolean, join: Long? = null, callback: (List<String>) -> Unit) {
        var thread = thread {
            // Prevent the settings from being loaded twice
            synchronized(loadingLock) {
                if (useCached && autoSettingFiles != null) {
                    callback(autoSettingFiles!!)
                    return@thread
                }

                try {
                    val json = JsonParser().parse(HttpUtils.get(
                            // TODO: Add another way to get all settings
                            "https://api.github.com/repos/CCBlueX/LiquidCloud/contents/LiquidBounce/settings"
                    ))

                    val autoSettings: MutableList<String> = mutableListOf()

                    if (json is JsonArray) {
                        for (setting in json)
                            autoSettings.add(setting.asJsonObject["name"].asString)
                    }

                    callback(autoSettings)

                    this.autoSettingFiles = autoSettings
                } catch (e: Exception) {
                    chat("Failed to fetch auto settings list.")
                }
            }
        }

        if (join != null) {
            thread.join(join)
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("list", "load").filter { it.startsWith(args[0], true) }
            2 -> {
                if (args[0].equals("load", true)) {
                    if (autoSettingFiles == null) {
                        this.loadSettings(true, 500) {}
                    }

                    if (autoSettingFiles != null) {
                        return autoSettingFiles!!.filter { it.startsWith(args[1], true) }
                    }
                }
                return emptyList()
            }
            else -> emptyList()
        }
    }
}