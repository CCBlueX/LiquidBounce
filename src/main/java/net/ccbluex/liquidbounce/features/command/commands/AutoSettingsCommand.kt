/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

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
import kotlin.concurrent.thread

object AutoSettingsCommand : Command("autosettings", "autosetting", "settings", "setting", "config") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val usedAlias = args[0].lowercase()

        if (args.size <= 1) {
            chatSyntax("$usedAlias <load/list/upload/report>")

            return
        }

        when (args[1].lowercase()) {
            // Load subcommand
            "load" -> {
                if (args.size < 3) {
                    chatSyntax("$usedAlias load <name/url>")
                    return
                }

                thread {
                    runCatching {
                        chat("Loading settings...")

                        // Load settings and apply them
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

            // Report subcommand
            "report" -> {
                if (args.size < 3) {
                    chatSyntax("$usedAlias report <name>")
                    return
                }

                thread {
                    runCatching {
                        val response = ClientApi.reportSettings(args[2])

                        when (response.status) {
                            Status.SUCCESS -> chat("§6${response.message}")
                            Status.ERROR -> chat("§c${response.message}")
                        }
                    }.onFailure {
                        LOGGER.error("Failed to report settings", it)
                        chat("Failed to report settings: ${it.message}")
                    }
                }
            }

            // Report subcommand
            "upload" -> {
                val option = if (args.size > 3) StringUtils.toCompleteString(args, 3).lowercase() else "values"
                val all = "all" in option
                val values = all || "values" in option
                val binds = all || "binds" in option
                val states = all || "states" in option

                if (!values && !binds && !states) {
                    chatSyntax("$usedAlias upload [all/values/binds/states]...")
                    return
                }

                thread {
                    runCatching {
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
                    }.onFailure {
                        LOGGER.error("Failed to upload settings", it)
                        chat("Failed to upload settings: ${it.message}")
                    }
                }
            }

            // List subcommand
            "list" -> {
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
            1 -> listOf("list", "load", "upload", "report").filter { it.startsWith(args[0], true) }
            2 -> {
                if (args[0].equals("load", true) || args[0].equals("report", true)) {
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