/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.Module
import org.lwjgl.input.Keyboard

class BindCommand : Command("bind")
{
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		val thePlayer = mc.thePlayer

		if (args.size > 1)
		{
			// Get module by name
			val module = LiquidBounce.moduleManager.getModule(args[1])

			if (module == null)
			{
				chat(thePlayer, "Module \u00A7a\u00A7l" + args[1] + "\u00A73 not found.")
				return
			}

			val keyBinds = module.keyBinds
			if (args.size > 2)
			{
				when (args[2].toLowerCase())
				{
					"add" -> if (args.size > 3)
					{
						val key = Keyboard.getKeyIndex(args[3].toUpperCase())
						if (key != Keyboard.KEY_NONE) keyBinds.add(key)

						chat(thePlayer, "Bound module \u00A7a\u00A7l${module.name}\u00A73 to key(s) \u00A7a\u00A7l${keyBinds.joinToString { Keyboard.getKeyName(it) }}\u00A73.")
						playEdit()
						return
					}

					"remove" -> if (args.size > 3)
					{
						val key = Keyboard.getKeyIndex(args[3].toUpperCase())
						if (!keyBinds.remove(key))
						{
							chat(thePlayer, "Module \u00A7a\u00A7l${module.name}\u00A73 hadn't bound to key \u00A7a\u00A7l${Keyboard.getKeyName(key)}\u00A73.")
							return
						}

						if (keyBinds.isEmpty()) chat(thePlayer, "Took all bounds from module \u00A7a\u00A7l${module.name}\u00A73.") else chat(thePlayer, "Bound module \u00A7a\u00A7l${module.name}\u00A73 to key(s) \u00A7a\u00A7l${keyBinds.joinToString { Keyboard.getKeyName(it) }}\u00A73.")
						playEdit()
					}

					"clear" ->
					{
						keyBinds.clear()
						chat(thePlayer, "Took all bounds from module \u00A7a\u00A7l${module.name}\u00A73.")
						playEdit()
						return
					}
				}
			}
			else chat(thePlayer, "Module \u00A7a\u00A7l${module.name}\u00A73 is bound to key(s) \u00A7a\u00A7l${keyBinds.joinToString { Keyboard.getKeyName(it) }}\u00A73.")
		}

		chatSyntax(thePlayer, arrayOf("<module> <add/remove> <key>", "<module> <clear>"))
	}

	override fun tabComplete(args: Array<String>): List<String>
	{
		if (args.isEmpty()) return emptyList()

		return when (args.size)
		{
			1 -> LiquidBounce.moduleManager.modules.map(Module::name).filter { it.startsWith(args[0], true) }.toList()
			2 -> listOf("add", "remove", "clear").filter { it.startsWith(args[1], ignoreCase = true) }
			else -> emptyList()
		}
	}
}
