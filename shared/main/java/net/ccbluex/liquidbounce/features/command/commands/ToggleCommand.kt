/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module

class ToggleCommand : Command("toggle", "t")
{
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		val thePlayer = mc.thePlayer

		if (args.size > 1)
		{
			val module = LiquidBounce.moduleManager.getModule(args[1])

			if (module == null)
			{
				chat(thePlayer, "Module '${args[1]}' not found.")
				return
			}

			if (args.size > 2)
			{
				val newState = args[2].toLowerCase()

				if (newState == "on" || newState == "off")
				{
					module.state = newState == "on"
				}
				else
				{
					chatSyntax(thePlayer, "toggle <module> [on/off]")
					return
				}
			}
			else
			{
				module.toggle()
			}

			chat(thePlayer, "${if (module.state) "Enabled" else "Disabled"} module \u00A78${module.name}\u00A73.")
			return
		}

		chatSyntax(thePlayer, "toggle <module> [on/off]")
	}

	override fun tabComplete(args: Array<String>): List<String>
	{
		if (args.isEmpty()) return emptyList()

		val moduleName = args[0]

		return when (args.size)
		{
			1 -> LiquidBounce.moduleManager.modules.map(Module::name).filter { it.startsWith(moduleName, true) }.toList()
			else -> emptyList()
		}
	}
}
