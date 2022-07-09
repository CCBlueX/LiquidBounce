/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.font.Fonts

class ReloadCommand : Command("reload", "configreload")
{
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		val thePlayer = mc.thePlayer

		chat(thePlayer, "Reloading...")

		// Reload Commands
		chat(thePlayer, "\u00A7c\u00A7lReloading commands...")
		LiquidBounce.commandManager = CommandManager()
		LiquidBounce.commandManager.registerCommands()
		LiquidBounce.isStarting = true
		LiquidBounce.scriptManager.disableScripts()
		LiquidBounce.scriptManager.unloadScripts()

		val moduleManager = LiquidBounce.moduleManager
		for (module in moduleManager.modules) moduleManager.generateCommand(module)

		// Reload Scripts
		chat(thePlayer, "\u00A7c\u00A7lReloading scripts...")
		LiquidBounce.scriptManager.reloadScripts()

		// Reload Fonts
		chat(thePlayer, "\u00A7c\u00A7lReloading fonts...")
		Fonts.loadFonts()

		val fileManager = LiquidBounce.fileManager

		// Reload Modules
		chat(thePlayer, "\u00A7c\u00A7lReloading modules...")
		FileManager.loadConfig(fileManager.modulesConfig)
		LiquidBounce.isStarting = false

		// Reload Values
		chat(thePlayer, "\u00A7c\u00A7lReloading values...")
		FileManager.loadConfig(fileManager.valuesConfig)

		// Reload Accounts
		chat(thePlayer, "\u00A7c\u00A7lReloading accounts...")
		FileManager.loadConfig(fileManager.accountsConfig)

		// Reload Friends
		chat(thePlayer, "\u00A7c\u00A7lReloading friends...")
		FileManager.loadConfig(fileManager.friendsConfig)

		// Reload XRay
		chat(thePlayer, "\u00A7c\u00A7lReloading xray...")
		FileManager.loadConfig(fileManager.xrayConfig)

		// Reload HUD
		chat(thePlayer, "\u00A7c\u00A7lReloading HUD...")
		FileManager.loadConfig(fileManager.hudConfig)

		// Reload Click GUI
		chat(thePlayer, "\u00A7c\u00A7lReloading ClickGUI...")
		LiquidBounce.clickGui = ClickGui()
		FileManager.loadConfig(fileManager.clickGuiConfig)

		chat(thePlayer, "Reloaded.")
	}
}
