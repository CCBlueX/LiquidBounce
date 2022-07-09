/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.misc.StringUtils

class SayCommand : Command("say")
{
	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		val thePlayer = mc.thePlayer ?: return

		if (args.size > 1)
		{
			thePlayer.sendChatMessage(StringUtils.toCompleteString(args, 1))
			chat(thePlayer, "Message was sent to the chat.")
			return
		}

		chatSyntax(thePlayer, "say <message...>")
	}
}
