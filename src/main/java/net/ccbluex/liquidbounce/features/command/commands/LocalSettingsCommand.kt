/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands


import kotlinx.coroutines.*
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

object LocalSettingsCommand : Command("localsettings", "localsetting", "localconfig") {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val usedAlias = args[0].lowercase()

        if (args.size <= 1) {
            chatSyntax("$usedAlias <load/save/list/delete/folder>")
            return
        }

        GlobalScope.launch {
            when (args[1].lowercase()) {
                "load" -> loadSettings(args)
                "save" -> saveSettings(args)
                "delete" -> deleteSettings(args)
                "list" -> listSettings()
                "folder" -> openSettingsFolder()
                else -> chatSyntax("$usedAlias <load/save/list/delete/folder>")
            }
        }
    }

    private suspend fun loadSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 2) {
                chatSyntax("${args[0].lowercase()} load <name>")
                return@withContext
            }

            val settingsFile = File(settingsDir, args[2] + ".txt")

            if (!settingsFile.exists()) {
                chat("§cSettings file does not exist! §e(Ensure its .txt)")
                return@withContext
            }

            try {
                chat("§9Loading settings...")
                val settings = settingsFile.readText()
                chat("§9Set settings...")
                SettingsUtils.applyScript(settings)
                chat("§6Settings applied successfully.")
                addNotification(Notification("Updated Settings"))
                playEdit()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun saveSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 2) {
                chatSyntax("${args[0].lowercase()} save <name> [all/values/binds/states]...")
                return@withContext
            }

            val settingsFile = File(settingsDir, args[2] + ".txt")

            try {
                if (settingsFile.exists())
                    settingsFile.delete()

                settingsFile.createNewFile()

                val option = if (args.size > 3) StringUtils.toCompleteString(args, 3).lowercase() else "default"
                val all = "all" in option
                val default = "default" in option
                val values = all || default || "values" in option
                val binds = all || "binds" in option
                val states = all || default || "states" in option

                if (!values && !binds && !states) {
                    chatSyntaxError()
                    return@withContext
                }

                chat("§9Creating settings...")
                val settingsScript = SettingsUtils.generateScript(values, binds, states)

                chat("§9Saving settings...")
                settingsFile.writeText(settingsScript)

                chat("§6Settings saved successfully.")
            } catch (throwable: Throwable) {
                chat("§cFailed to create local config: §3${throwable.message}")
                LOGGER.error("Failed to create local config.", throwable)
            }
        }
    }

    private suspend fun deleteSettings(args: Array<String>) {
        withContext(Dispatchers.IO) {
            if (args.size <= 2) {
                chatSyntax("${args[0].lowercase()} delete <name>")
                return@withContext
            }

            val settingsFile = File(settingsDir, args[2] + ".txt")

            if (!settingsFile.exists()) {
                chat("§cSettings file does not exist!")
                return@withContext
            }

            settingsFile.delete()
            chat("§6Settings file deleted successfully.")
        }
    }

    private suspend fun listSettings() {
        withContext(Dispatchers.IO) {
            chat("§cSettings:")

            val settings = settingsDir.listFiles() ?: return@withContext

            for (file in settings) {
                chat("> " + file.name.removeSuffix(".txt"))
            }
        }
    }

    private suspend fun openSettingsFolder() {
        withContext(Dispatchers.IO) {
            try {
                Desktop.getDesktop().open(settingsDir)
            } catch (e: IOException) {
                LOGGER.error("Failed to open settings folder.", e)
                chat("§cFailed to open settings folder.")
            }
        }
    }


    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("delete", "list", "load", "save", "folder").filter { it.startsWith(args[0], true) }

            2 -> {
                when (args[0].lowercase()) {
                    "delete", "load", "save" -> {
                        val settings = settingsDir.listFiles() ?: return emptyList()
                        settings.map { it.name.removeSuffix(".txt") }.filter { it.startsWith(args[1], true) }
                    }
                    else -> emptyList()
                }
            }

            3 -> {
                when (args[0].lowercase()) {
                    "save" -> listOf("all", "default","values", "binds", "states").filter { it.startsWith(args[2], true) }
                    else -> emptyList()
                }
            }

            else -> emptyList()
        }
    }
}