/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands


import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.file.FileManager.settingsDir
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.SettingsUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import java.awt.Desktop
import java.io.File
import java.io.IOException

object LocalAutoSettingsCommand : Command("localautosettings", "localsetting", "localsettings", "localconfig") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val usedAlias = args[0].lowercase()

        if (args.size <= 1) {
            chatSyntax("$usedAlias <load/save/list/delete/folder>")
            return
        }

        when (args[1].lowercase()) {
            "load" -> {
                if (args.size <= 2) {
                    chatSyntax("$usedAlias load <name>")
                    return
                }

                val scriptFile = File(settingsDir, args[2])

                if (!scriptFile.exists()) {
                    chat("§cSettings file does not exist!")
                    return
                }

                try {
                    chat("§9Loading settings...")
                    val settings = scriptFile.readText()
                    chat("§9Set settings...")
                    SettingsUtils.applyScript(settings)
                    chat("§6Settings applied successfully.")
                    addNotification(Notification("Updated Settings"))
                    playEdit()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            "save" -> {
                if (args.size <= 2) {
                    chatSyntax("$usedAlias save <name> [all/values/binds/states]...")
                    return
                }

                val scriptFile = File(settingsDir, args[2])

                try {
                    if (scriptFile.exists())
                        scriptFile.delete()

                    scriptFile.createNewFile()

                    val option = if (args.size > 3) StringUtils.toCompleteString(args, 3).lowercase() else "values"
                    val all = "all" in option
                    val values = all || "values" in option
                    val binds = all || "binds" in option
                    val states = all || "states" in option

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
                    LOGGER.error("Failed to create local config.", throwable)
                }
            }

            "delete" -> {
                if (args.size <= 2) {
                    chatSyntax("$usedAlias delete <name>")
                    return
                }

                val scriptFile = File(settingsDir, args[2])

                if (!scriptFile.exists()) {
                    chat("§cSettings file does not exist!")
                    return
                }

                scriptFile.delete()
                chat("§6Settings file deleted successfully.")
            }

            "list" -> {
                chat("§cSettings:")

                val settings = getLocalSettings() ?: return

                for (file in settings)
                    chat("> " + file.name)
            }

            "folder" -> {
                Desktop.getDesktop().open(settingsDir)
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("delete", "list", "load", "save").filter { it.startsWith(args[0], true) }

            2 ->
                when (args[0].lowercase()) {
                    "delete", "load" -> {
                        val settings = getLocalSettings() ?: return emptyList()

                        settings
                            .map { it.name }
                            .filter { it.startsWith(args[1], true) }
                    }

                    else -> emptyList()
                }

            else -> emptyList()
        }
    }

    private fun getLocalSettings() = settingsDir.listFiles()
}