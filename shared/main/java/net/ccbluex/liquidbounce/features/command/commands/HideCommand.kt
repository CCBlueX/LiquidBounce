/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils

class HideCommand : Command("hide")
{

	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		if (args.size > 1)
		{
			when
			{
				args[1].equals("list", true) ->
				{
					chat("\u00A7c\u00A7lHidden")
					LiquidBounce.moduleManager.modules.asSequence().filter { !it.array }.forEach {
						ClientUtils.displayChatMessage("\u00A76> \u00A7c${it.name}")
					}
					return
				}

				args[1].equals("clear", true) ->
				{
					for (module in LiquidBounce.moduleManager.modules) module.array = true

					chat("Cleared hidden modules.")
					return
				}

				args[1].equals("reset", true) ->
				{
					for (module in LiquidBounce.moduleManager.modules) module.array = module::class.java.getAnnotation(ModuleInfo::class.java).array

					chat("Reset hidden modules.")
					return
				}

				else ->
				{ // Get module by name
					val module = LiquidBounce.moduleManager.getModule(args[1])

					if (module == null)
					{
						chat("Module \u00A7a\u00A7l${args[1]}\u00A73 not found.")
						return
					}

					// Find key by name and change
					module.array = !module.array

					// Response to user
					chat("Module \u00A7a\u00A7l${module.name}\u00A73 is now \u00A7a\u00A7l${if (module.array) "visible" else "invisible"}\u00A73 on the array list.")
					playEdit()
					return
				}
			}
		}

		chatSyntax("hide <module/list/clear/reset>")
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
