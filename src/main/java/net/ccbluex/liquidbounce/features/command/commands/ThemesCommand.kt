/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.file.FileManager.dir
import net.ccbluex.liquidbounce.file.FileManager.hudConfig
import net.ccbluex.liquidbounce.file.FileManager.loadConfig
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import java.io.File
import java.io.IOException

object ThemesCommand : Command("themes", "theme", "cloudthemes", "cloudtheme") {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val usedAlias = args[0].lowercase()

        if (args.size <= 1) {
            chatSyntax("$usedAlias <load/list>")
            return
        }


        GlobalScope.launch {
            when (args[1].lowercase()) {
                "load" -> {
                    if (args.size <= 2) {
                        chatSyntax("$usedAlias load <name>")
                        return@launch
                    }

                    loadTheme(args[2])
                }

                "list" -> {
                    listThemes()
                }

                else -> chatSyntax("$usedAlias <load/list>")
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("list", "load").filter { it.startsWith(args[0], true) }

            else -> emptyList()
        }
    }

    private suspend fun getRemoteThemes(): List<String>? {
        return try {
            val remoteThemes = "$CLIENT_CLOUD/themes/legacy"
            val (body, code) = fetchDataAsync(remoteThemes)
            if (code == 200) { // Yeah, ik this is not correct.. it is why its on drafted for now.
                body?.split("\n")?.filter { it.isNotBlank() && it.isNotEmpty() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadTheme(themeName: String) {
        withContext(Dispatchers.IO) {
            try {
                val themes = getRemoteThemes()
                if (themes.isNullOrEmpty() || "$themeName.json" !in themes) {
                    chat("§cTheme file does not exist!")
                    return@withContext
                }

                val themeFileUrl = "$themeName.json"
                val hudFile = File(dir, "hud.json")

                val (themeContent, _) = fetchDataAsync(themeFileUrl)

                if (themeContent == null) {
                    chat("§cFailed to load theme content.")
                    return@withContext
                }

                chat("§9Loading theme...")
                hudFile.writeText(themeContent)
                loadConfig(hudConfig)
                chat("§6Theme applied successfully.")
                addNotification(Notification("Updated Theme"))
                playEdit()
            } catch (e: IOException) {
                chat("Failed to load theme: ${e.message}")
            }
        }
    }

    private suspend fun fetchDataAsync(url: String): Pair<String?, Int> {
        return withContext(Dispatchers.IO) {
            val (body, code) = HttpUtils.request(url, "GET")
            Pair(body, code)
        }
    }

    private suspend fun listThemes() {
        withContext(Dispatchers.IO) {
            val themes = getRemoteThemes()
            if (themes.isNullOrEmpty()) {
                chat("§cNo themes found.")
                return@withContext
            }

            chat("§cThemes:")
            themes.forEach { theme ->
                chat("> ${theme.removeSuffix(".json")}")
            }
        }
    }
}