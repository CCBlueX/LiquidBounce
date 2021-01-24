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
		chat("Reloading...")
		chat("\u00A7c\u00A7lReloading commands...")
		LiquidBounce.commandManager = CommandManager()
		LiquidBounce.commandManager.registerCommands()
		LiquidBounce.isStarting = true
		LiquidBounce.scriptManager.disableScripts()
		LiquidBounce.scriptManager.unloadScripts()
		for (module in LiquidBounce.moduleManager.modules) LiquidBounce.moduleManager.generateCommand(module)
		chat("\u00A7c\u00A7lReloading scripts...")
		LiquidBounce.scriptManager.reloadScripts()
		chat("\u00A7c\u00A7lReloading fonts...")
		Fonts.loadFonts()
		chat("\u00A7c\u00A7lReloading modules...")
		FileManager.loadConfig(LiquidBounce.fileManager.modulesConfig)
		LiquidBounce.isStarting = false
		chat("\u00A7c\u00A7lReloading values...")
		FileManager.loadConfig(LiquidBounce.fileManager.valuesConfig)
		chat("\u00A7c\u00A7lReloading accounts...")
		FileManager.loadConfig(LiquidBounce.fileManager.accountsConfig)
		chat("\u00A7c\u00A7lReloading friends...")
		FileManager.loadConfig(LiquidBounce.fileManager.friendsConfig)
		chat("\u00A7c\u00A7lReloading xray...")
		FileManager.loadConfig(LiquidBounce.fileManager.xrayConfig)
		chat("\u00A7c\u00A7lReloading HUD...")
		FileManager.loadConfig(LiquidBounce.fileManager.hudConfig)
		chat("\u00A7c\u00A7lReloading ClickGUI...")
		LiquidBounce.clickGui = ClickGui()
		FileManager.loadConfig(LiquidBounce.fileManager.clickGuiConfig)
		chat("Reloaded.")
	}
}
