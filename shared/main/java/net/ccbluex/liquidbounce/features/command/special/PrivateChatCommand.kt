package net.ccbluex.liquidbounce.features.command.special

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.misc.LiquidChat
import net.ccbluex.liquidbounce.utils.misc.StringUtils

class PrivateChatCommand : Command("pchat", "privatechat", "lcpm")
{

	private val lChat = LiquidBounce.moduleManager[LiquidChat::class.java] as LiquidChat

	/**
	 * Execute commands with provided [args]
	 */
	override fun execute(args: Array<String>)
	{
		if (args.size > 2)
		{
			if (!lChat.state)
			{
				chat("\u00A7cError: \u00A77LiquidChat is disabled!")
				return
			}

			if (!lChat.client.isConnected())
			{
				chat("\u00A7cError: \u00A7LiquidChat is currently not connected to the server!")
				return
			}

			val target = args[1]
			val message = StringUtils.toCompleteString(args, 2)

			lChat.client.sendPrivateMessage(target, message)
			chat("Message was successfully sent.")
		} else chatSyntax("pchat <username> <message>")
	}
}
