/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import org.lwjgl.input.Keyboard

class BindCommand : Command("bind")
{
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		if (args.size > 2)
		{ // Get module by name
			val module = LiquidBounce.moduleManager.getModule(args[1])

			if (module == null)
			{
				chat("Module \u00A7a\u00A7l" + args[1] + "\u00A73 not found.")
				return
			} // Find key by name and change
			val key = Keyboard.getKeyIndex(args[2].toUpperCase())
			module.keyBind = key

			// Response to user
			chat("Bound module \u00A7a\u00A7l${module.name}\u00A73 to key \u00A7a\u00A7l${Keyboard.getKeyName(key)}\u00A73.")
			LiquidBounce.hud.addNotification(Notification("Key Binding", "Bound ${module.name} to ${Keyboard.getKeyName(key)}", null))
			playEdit()
			return
		}

		chatSyntax(arrayOf("<module> <key>", "<module> none"))
	}

	override fun tabComplete(args: Array<String>): List<String>
	{
		if (args.isEmpty()) return emptyList()

		val moduleName = args[0]

		return when (args.size)
		{
			1 -> LiquidBounce.moduleManager.modules.asSequence().map(Module::name).filter { it.startsWith(moduleName, true) }.toList()
			else -> emptyList()
		}
	}
}
