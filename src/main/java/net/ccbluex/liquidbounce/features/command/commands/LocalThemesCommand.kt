/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.file.FileManager.dir
import net.ccbluex.liquidbounce.file.FileManager.themesDir
import net.ccbluex.liquidbounce.file.FileManager.hudConfig
import net.ccbluex.liquidbounce.file.FileManager.loadConfig
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import java.awt.Desktop
import java.io.File
import java.io.FileFilter
import java.io.IOException

object LocalThemesCommand : Command("localthemes", "localtheme") {
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

                val themeFile = File(themesDir, args[2] + ".json")
                val hudFile = File(dir, "hud.json")

                if (!themeFile.exists()) {
                    chat("§cTheme file does not exist!")
                    return
                }

                try {
                    chat("§9Loading theme...")
                    themeFile.copyTo(hudFile, true)
                    loadConfig(hudConfig)
                    chat("§6Theme applied successfully.")
                    addNotification(Notification("Updated Theme"))
                    playEdit()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            "save" -> {
                if (args.size <= 2) {
                    chatSyntax("$usedAlias save <name>...")
                    return
                }

                val themeFile = File(themesDir, args[2] + ".json")

                try {
                    if (themeFile.exists())
                        themeFile.delete()

                    chat("§9Creating theme...")
                    themeFile.createNewFile()

                    chat("§9Saving theme...")
                    File(dir, "hud.json").copyTo(themeFile, true)
                    loadConfig(hudConfig)

                    chat("§6Theme saved successfully.")
                } catch (throwable: Throwable) {
                    chat("§cFailed to create local theme: §3${throwable.message}")
                    LOGGER.error("Failed to create local theme.", throwable)
                }
            }

            "delete" -> {
                if (args.size <= 2) {
                    chatSyntax("$usedAlias delete <name>")
                    return
                }

                val themeFile = File(themesDir, args[2] + ".json")

                if (!themeFile.exists()) {
                    chat("§cTheme file does not exist!")
                    return
                }

                themeFile.delete()
                chat("§6Theme file deleted successfully.")
            }

            "list" -> {
                chat("§cThemes:")

                val themes = getLocalThemes() ?: return

                for (file in themes) {
                    val fileName = file.name.removeSuffix(".json")

                    chat("> $fileName")
                }
            }

            "folder" -> {
                Desktop.getDesktop().open(themesDir)
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("delete", "list", "load", "save", "folder").filter { it.startsWith(args[0], true) }

            2 ->
                when (args[0].lowercase()) {
                    "delete", "load", "save" -> {
                        val themes = getLocalThemes() ?: return emptyList()

                        themes
                            .map { it.name.replace(".json", "") }
                            .filter { it.startsWith(args[1], true) }
                    }

                    else -> emptyList()
                }

            else -> emptyList()
        }
    }

    private fun getLocalThemes() = themesDir.listFiles(FileFilter { it.extension == "json" })
}
