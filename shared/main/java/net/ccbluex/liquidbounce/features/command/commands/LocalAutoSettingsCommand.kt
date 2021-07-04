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

class LocalAutoSettingsCommand : Command("localautosettings", "localsetting", "localsettings", "localconfig")
{
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		val thePlayer = mc.thePlayer

		if (args.size > 1)
		{
			when (args[1].toLowerCase())
			{
				"load" ->
				{
					if (args.size > 2)
					{
						val scriptFile = File(LiquidBounce.fileManager.settingsDir, args[2])

						if (scriptFile.exists())
						{
							try
							{
								chat(thePlayer, "\u00A79Loading settings...")
								val settings = scriptFile.readText()
								chat(thePlayer, "\u00A79Set settings...")
								SettingsUtils.executeScript(settings)
								chat(thePlayer, "\u00A76Settings applied successfully.")
								LiquidBounce.hud.addNotification(Notification("Local Autosettings", "Updated Settings", null))
								playEdit()
							}
							catch (e: IOException)
							{
								e.printStackTrace()
							}

							return
						}

						chat(thePlayer, "\u00A7cSettings file does not exist!")
						return
					}

					chatSyntax(thePlayer, "localautosettings load <name>")
					return
				}

				"save" ->
				{
					if (args.size > 2)
					{
						val scriptFile = File(LiquidBounce.fileManager.settingsDir, args[2])

						try
						{
							if (scriptFile.exists()) scriptFile.delete()
							scriptFile.createNewFile()

							val option = if (args.size > 3) StringUtils.toCompleteString(args, 3).toLowerCase() else "values"
							val (values, binds, states) = when (option)
							{
								"all" -> Triple(first = true, second = true, third = true)
								"values" -> Triple(first = true, second = false, third = false)
								"binds" -> Triple(false, second = true, third = false)
								"states" -> Triple(first = true, second = true, third = true)
								"exceptbinds" -> Triple(first = true, second = false, third = true)

								else ->
								{
									chatSyntaxError(thePlayer)
									return
								}
							}

							chat(thePlayer, "\u00A79Creating settings...")
							val settingsScript = SettingsUtils.generateScript(values, binds, states)
							chat(thePlayer, "\u00A79Saving settings...")
							scriptFile.writeText(settingsScript)
							chat(thePlayer, "\u00A76Settings saved successfully.")
						}
						catch (throwable: Throwable)
						{
							chat(thePlayer, "\u00A7cFailed to create local config: \u00A73${throwable.message}")
							ClientUtils.logger.error("Failed to create local config.", throwable)
						}
						return
					}

					chatSyntax(thePlayer, "localsettings save <name> [all/values/binds/states/exceptbinds]...")
					return
				}

				"delete" ->
				{
					if (args.size > 2)
					{
						val scriptFile = File(LiquidBounce.fileManager.settingsDir, args[2])

						if (scriptFile.exists())
						{
							scriptFile.delete()
							chat(thePlayer, "\u00A76Settings file deleted successfully.")
							return
						}

						chat(thePlayer, "\u00A7cSettings file does not exist!")
						return
					}

					chatSyntax(thePlayer, "localsettings delete <name>")
					return
				}

				"list" ->
				{
					chat(thePlayer, "\u00A7cSettings:")

					val settings = getLocalSettings() ?: return

					for (file in settings) chat(thePlayer, "> " + file.name)
					return
				}
			}
		}

		chatSyntax(thePlayer, "localsettings <load/save/list/delete>")
	}

	override fun tabComplete(args: Array<String>): List<String>
	{
		if (args.isEmpty()) return emptyList()

		return when (args.size)
		{
			1 -> listOf("delete", "list", "load", "save").filter { it.startsWith(args[0], true) }

			2 ->
			{
				when (args[0].toLowerCase())
				{
					"delete", "load" ->
					{
						val settings = getLocalSettings() ?: return emptyList()

						return settings.map(File::getName).filter { it.startsWith(args[1], true) }.toList()
					}
				}
				return emptyList()
			}

			else -> emptyList()
		}
	}

	private fun getLocalSettings(): Array<File>? = LiquidBounce.fileManager.settingsDir.listFiles()
}
