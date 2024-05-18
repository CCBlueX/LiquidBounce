/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.ClientApi
import net.ccbluex.liquidbounce.api.Status
import net.ccbluex.liquidbounce.api.autoSettingsList
import net.ccbluex.liquidbounce.api.loadSettings
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.SettingsUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.get
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

object SettingsCommand : Command("autosettings", "autosetting", "settings", "setting", "config") {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val usedAlias = args[0].lowercase()

        if (args.size <= 1) {
            chatSyntax("$usedAlias <load/list/upload/report>")
            return
        }

        GlobalScope.launch {
            when (args[1].lowercase()) {
                "load" -> loadSettings(args)
                "report" -> reportSettings(args)
                "upload" -> uploadSettings(args)
                "list" -> listSettings()
                else -> chatSyntax("$usedAlias <load/list/upload/report>")
            }
        }
    }

    // Load subcommand
    private suspend fun loadSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size < 3) {
                chatSyntax("${args[0].lowercase()} load <name/url>")
                return@withContext
            }

            try {
                val settings = if (args[2].startsWith("http")) {
                    val (text, code) = get(args[2])
                    if (code != 200) {
                        error(text)
                    }

                    text
                } else {
                    ClientApi.requestSettingsScript(args[2])
                }

                chat("Applying settings...")
                SettingsUtils.applyScript(settings)
                chat("§6Settings applied successfully")
                addNotification(Notification("Updated Settings"))
                playEdit()
            } catch (e: Exception) {
                LOGGER.error("Failed to load settings", e)
                chat("Failed to load settings: ${e.message}")
            }
        }
    }

    // Report subcommand
    private suspend fun reportSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size < 3) {
                chatSyntax("${args[0].lowercase()} report <name>")
                return@withContext
            }

            try {
                val response = ClientApi.reportSettings(args[2])
                when (response.status) {
                    Status.SUCCESS -> chat("§6${response.message}")
                    Status.ERROR -> chat("§c${response.message}")
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to report settings", e)
                chat("Failed to report settings: ${e.message}")
            }
        }
    }

    // Upload subcommand
    private suspend fun uploadSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            val option = if (args.size > 3) StringUtils.toCompleteString(args, 3).lowercase() else "all"
            val all = "all" in option
            val values = all || "values" in option
            val binds = all || "binds" in option
            val states = all || "states" in option

            if (!values && !binds && !states) {
                chatSyntax("${args[0].lowercase()} upload [all/values/binds/states]...")
                return@withContext
            }

            try {
                chat("§9Creating settings...")
                val settingsScript = SettingsUtils.generateScript(values, binds, states)
                chat("§9Uploading settings...")

                val serverData = mc.currentServerData ?: error("You need to be on a server to upload settings.")

                val name = "${LiquidBounce.clientCommit}-${serverData.serverIP.replace(".", "_")}"
                val response = ClientApi.uploadSettings(name, mc.session.username, settingsScript)

                when (response.status) {
                    Status.SUCCESS -> {
                        chat("§6${response.message}")
                        chat("§9Token: §6${response.token}")

                        // Store token in clipboard
                        val stringSelection = StringSelection(response.token)
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(stringSelection, stringSelection)
                    }
                    Status.ERROR -> chat("§c${response.message}")
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to upload settings", e)
                chat("Failed to upload settings: ${e.message}")
            }
        }
    }

    // List subcommand
    private suspend fun listSettings() {
        withContext(Dispatchers.IO) {
            chat("Loading settings...")
            loadSettings(false) {
                for (setting in it) {
                    chat("> ${setting.settingId} (Last updated: ${setting.date}, Status: ${setting.statusType.displayName})")
                }
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) {
            return emptyList()
        }

        return when (args.size) {
            1 -> listOf("list", "load", "upload", "report").filter { it.startsWith(args[0], true) }
            2 -> {
                when (args[0].lowercase()) {
                    "load", "report" -> {
                        if (autoSettingsList == null) {
                            loadSettings(true, 500) {}
                        }

                        return autoSettingsList?.filter { it.settingId.startsWith(args[1], true) }?.map { it.settingId }
                            ?: emptyList()
                    }
                    "upload" -> {
                        return listOf("all", "values", "binds", "states").filter { it.startsWith(args[1], true) }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}
