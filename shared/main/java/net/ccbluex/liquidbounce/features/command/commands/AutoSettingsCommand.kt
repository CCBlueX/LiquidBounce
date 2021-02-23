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
import net.ccbluex.liquidbounce.utils.WorkerUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import kotlin.concurrent.thread

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

		when (args[1].toLowerCase())
		{

			// Load subcommand
			"load" ->
			{
				if (args.size < 3)
				{
					chatSyntax(thePlayer, "settings load <name/url>")
					return
				}

				// Settings url
				val url = if (args[2].startsWith("http")) args[2]
				else "${LiquidBounce.CLIENT_CLOUD}/settings/${args[2].toLowerCase()}"

				chat(thePlayer, "Loading settings...")

				WorkerUtils.workers.execute {
					try
					{ // Load settings and apply them
						val settings = HttpUtils[url]

						chat(thePlayer, "Applying settings...")
						SettingsUtils.executeScript(settings)
						chat(thePlayer, "\u00A76Settings applied successfully")
						LiquidBounce.hud.addNotification(Notification("Autosettings", "Updated Settings", null))
						playEdit()
					}
					catch (exception: Exception)
					{
						exception.printStackTrace()
						chat(thePlayer, "Failed to fetch auto settings.")
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
		val thread = thread {

			// Prevent the settings from being loaded twice
			synchronized(loadingLock) {
				if (useCached && autoSettingFiles != null)
				{
					callback(autoSettingFiles!!)
					return@thread
				}

				try
				{
					val json = JsonParser().parse(HttpUtils["https://api.github.com/repos/CCBlueX/LiquidCloud/contents/LiquidBounce/settings"])

					val autoSettings: MutableList<String> = mutableListOf()

					if (json is JsonArray) json.mapTo(autoSettings) { it.asJsonObject["name"].asString }

					callback(autoSettings)

					autoSettingFiles = autoSettings
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
				if (args[0].equals("load", ignoreCase = true)) if (autoSettingFiles == null) loadSettings(true, 500) {}
				else return autoSettingFiles!!.filter { it.startsWith(args[1], true) }.toList()

				return emptyList()
			}

			else -> emptyList()
		}
	}
}
