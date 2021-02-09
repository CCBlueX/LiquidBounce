/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.ClientUtils
import org.lwjgl.input.Keyboard

class BindsCommand : Command("binds")
{
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		val thePlayer = mc.thePlayer

		if (args.size > 1)
		{
			if (args[1].equals("clear", true))
			{
				for (module in LiquidBounce.moduleManager.modules) module.keyBind = Keyboard.KEY_NONE

				chat(thePlayer, "Removed all binds.")
				return
			}
		}

		chat(thePlayer, "\u00A7c\u00A7lBinds")

		LiquidBounce.moduleManager.modules.asSequence().filter { it.keyBind != Keyboard.KEY_NONE }.forEach {
			ClientUtils.displayChatMessage(thePlayer, "\u00A76> \u00A7c${it.name}: \u00A7a\u00A7l${Keyboard.getKeyName(it.keyBind)}")
		}

		chatSyntax(thePlayer, "binds clear")
	}
}
