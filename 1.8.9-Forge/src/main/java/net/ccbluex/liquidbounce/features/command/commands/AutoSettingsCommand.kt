package net.ccbluex.liquidbounce.features.command.commands

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.SettingsUtils
import net.ccbluex.liquidbounce.utils.misc.NetworkUtils
import java.net.URL
import kotlin.concurrent.thread

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class AutoSettingsCommand : Command("autosettings", arrayOf("setting", "settings", "config", "autosetting")) {

    private var autoSettings: JsonArray? = null

    override fun execute(args: Array<String>) {
        if (args.size <= 1) {
            chatSyntax("autosettings <load/list>")
            return
        }

        if (args[1].equals("list", ignoreCase = true)) {
            chat("Loading settings...")

            thread {
                val settings = this.getSettings()

                for (setting in settings)
                    chat("> " + setting.asJsonObject["name"].asString)
            }
            return
        } else if (args[1].equals("load", ignoreCase = true)) {
            if (args.size < 3) {
                chatSyntax("autosettings load <name/url>")
                return
            }

            thread {
                try {
                    val url = if (args[2].startsWith("http")) args[2] else "https://ccbluex.github.io/FileCloud/${LiquidBounce.CLIENT_NAME}/autosettings/${args[2].toLowerCase()}"

                    chat("Loading settings...")
                    val settings = URL(url).readText().lines().filter {
                        it.isNotEmpty() && !it.startsWith('#')
                    }
                    chat("Applying settings...")
                    SettingsUtils.executeScript(settings)
                    chat("§6Settings applied successfully")
                    LiquidBounce.CLIENT.hud.addNotification(Notification("Updated Settings"))
                    playEdit()
                } catch (exception: Exception) {
                    chat("Failed to fetch auto settings.")
                }
            }
            return
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("list", "load").filter { it.startsWith(args[0], true) }
            2 -> {
                if (args[0].equals("load", true)) {
                    return this.getSettings()
                        .map { it.asJsonObject["name"].asString }
                        .filter { it.startsWith(args[1], true) }
                }
                return emptyList()
            }
            else -> emptyList()
        }
    }

    private fun getSettings(): JsonArray {
        if (this.autoSettings != null) return this.autoSettings!!

        try {
            val listUrl = "https://api.github.com/repos/CCBlueX/FileCloud/contents/LiquidBounce/autosettings"
            val listElement = JsonParser().parse(NetworkUtils.readContent(listUrl))

            if (!listElement.isJsonArray)
                return JsonArray()

            this.autoSettings = listElement.asJsonArray
            return this.getSettings()
        } catch (e: Exception) {
            chat("Failed to fetch auto settings list.")
        }
        return JsonArray()
    }
}