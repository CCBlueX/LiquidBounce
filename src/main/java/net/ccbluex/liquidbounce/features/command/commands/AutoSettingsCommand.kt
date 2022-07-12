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
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotificationIcon
import net.ccbluex.liquidbounce.utils.SettingsUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.ccbluex.liquidbounce.utils.runAsync
import java.util.*
import kotlin.concurrent.thread

private const val SETTINGS_LIST_URL = "https://api.github.com/repos/CCBlueX/LiquidCloud/contents/LiquidBounce/settings"
private const val SETTING_URL = "${LiquidBounce.CLIENT_CLOUD}/settings/%s"

class AutoSettingsCommand : Command("autosettings", "setting", "settings", "config", "autosetting")
{
    private val loadingLock = Object()
    private var autoSettingFiles: MutableList<String>? = null

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>)
    {
        val thePlayer = mc.thePlayer

        if (args.size <= 1)
        {
            chatSyntax(thePlayer, "settings <load/list>")

            return
        }

        when (args[1].lowercase(Locale.getDefault()))
        {

            // Load subcommand
            "load" ->
            {
                if (args.size < 3)
                {
                    chatSyntax(thePlayer, "settings load <name/url>")
                    return
                }

                chat(thePlayer, "Loading settings...")
                val url = if (args[2].startsWith("http")) args[2] else SETTING_URL.format(args[2].lowercase(Locale.getDefault()))

                runAsync {
                    try
                    {
                        val settings = HttpUtils[url]

                        chat(thePlayer, "Applying settings...")
                        SettingsUtils.executeScript(settings)
                        chat(thePlayer, "\u00A76Settings applied successfully")
                        LiquidBounce.hud.addNotification(Notification(NotificationIcon.INFORMATION, "AutoSettings", arrayOf("Updated settings from", url)))
                        playEdit()
                    }
                    catch (e: Exception)
                    {
                        e.printStackTrace()
                        chat(thePlayer, "\u00A74Failed to fetch settings ($e)")
                        LiquidBounce.hud.addNotification(Notification(NotificationIcon.ERROR, "AutoSettings", arrayOf("Failed to fetch settings from", url, "$e")))
                    }
                }
            }

            // List subcommand
            "list" ->
            {
                chat(thePlayer, "Loading settings...")

                loadSettings(false) { it.map { setting -> "> $setting" }.forEach { setting -> chat(thePlayer, setting) } }
            }
        }
    }

    private fun loadSettings(useCached: Boolean, join: Long? = null, callback: (List<String>) -> Unit)
    {
        val autoSettingFiles = autoSettingFiles

        val thread = thread {

            // Prevent the settings from being loaded twice
            synchronized(loadingLock) {
                if (useCached && autoSettingFiles != null)
                {
                    callback(autoSettingFiles)
                    return@thread
                }

                try
                {
                    val json = JsonParser().parse(HttpUtils[SETTINGS_LIST_URL])

                    val autoSettings: MutableList<String> = mutableListOf()

                    if (json is JsonArray) json.mapTo(autoSettings) { it.asJsonObject["name"].asString }

                    callback(autoSettings)

                    this.autoSettingFiles = autoSettings
                }
                catch (e: Exception)
                {
                    chat(mc.thePlayer, "Failed to fetch auto settings list.")
                }
            }
        }

        if (join != null) thread.join(join)
    }

    override fun tabComplete(args: Array<String>): List<String>
    {
        if (args.isEmpty()) return emptyList()

        return when (args.size)
        {
            1 -> listOf("list", "load").filter { it.startsWith(args[0], ignoreCase = true) }

            2 ->
            {
                if (args[0].equals("load", ignoreCase = true)) autoSettingFiles?.filter { it.startsWith(args[1], true) }?.toList() ?: run { loadSettings(true, 500) {} }

                return emptyList()
            }

            else -> emptyList()
        }
    }
}
