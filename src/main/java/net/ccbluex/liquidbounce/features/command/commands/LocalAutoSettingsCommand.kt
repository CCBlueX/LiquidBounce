/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.SettingsUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import java.io.File
import java.io.IOException

class LocalAutoSettingsCommand : Command("localautosettings", "localsetting", "localsettings", "localconfig") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            when {
                args[1].equals("load", ignoreCase = true) -> {
                    if (args.size > 2) {
                        val scriptFile = File(LiquidBounce.fileManager.settingsDir, args[2])

                        if (scriptFile.exists()) {
                            try {
                                chat("§9Loading settings...")
                                val settings = scriptFile.readText()
                                chat("§9Set settings...")
                                SettingsUtils.executeScript(settings)
                                chat("§6Settings applied successfully.")
                                LiquidBounce.hud.addNotification(Notification("Updated Settings"))
                                playEdit()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            return
                        }

                        chat("§cSettings file does not exist!")
                        return
                    }

                    chatSyntax("localautosettings load <name>")
                    return
                }

                args[1].equals("save", ignoreCase = true) -> {
                    if (args.size > 2) {
                        val scriptFile = File(LiquidBounce.fileManager.settingsDir, args[2])

                        try {
                            if (scriptFile.exists())
                                scriptFile.delete()
                            scriptFile.createNewFile()

                            val option = if (args.size > 3) StringUtils.toCompleteString(args, 3).lowercase() else "values"
                            val values = option.contains("all") || option.contains("values")
                            val binds = option.contains("all") || option.contains("binds")
                            val states = option.contains("all") || option.contains("states")
                            if (!values && !binds && !states) {
                                chatSyntaxError()
                                return
                            }

                            chat("§9Creating settings...")
                            val settingsScript = SettingsUtils.generateScript(values, binds, states)
                            chat("§9Saving settings...")
                            scriptFile.writeText(settingsScript)
                            chat("§6Settings saved successfully.")
                        } catch (throwable: Throwable) {
                            chat("§cFailed to create local config: §3${throwable.message}")
                            ClientUtils.getLogger().error("Failed to create local config.", throwable)
                        }
                        return
                    }

                    chatSyntax("localsettings save <name> [all/values/binds/states]...")
                    return
                }

                args[1].equals("delete", ignoreCase = true) -> {
                    if (args.size > 2) {
                        val scriptFile = File(LiquidBounce.fileManager.settingsDir, args[2])

                        if (scriptFile.exists()) {
                            scriptFile.delete()
                            chat("§6Settings file deleted successfully.")
                            return
                        }

                        chat("§cSettings file does not exist!")
                        return
                    }

                    chatSyntax("localsettings delete <name>")
                    return
                }

                args[1].equals("list", ignoreCase = true) -> {
                    chat("§cSettings:")

                    val settings = this.getLocalSettings() ?: return

                    for (file in settings)
                        chat("> " + file.name)
                    return
                }
            }
        }
        chatSyntax("localsettings <load/save/list/delete>")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("delete", "list", "load", "save").filter { it.startsWith(args[0], true) }
            2 -> {
                when (args[0].lowercase()) {
                    "delete", "load" -> {
                        val settings = this.getLocalSettings() ?: return emptyList()

                        return settings
                            .map { it.name }
                            .filter { it.startsWith(args[1], true) }
                    }
                }
                return emptyList()
            }
            else -> emptyList()
        }
    }

    private fun getLocalSettings(): Array<File>? = LiquidBounce.fileManager.settingsDir.listFiles()
}