/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command

class PingCommand : Command("ping")
{
	override fun execute(args: Array<String>)
	{
		val thePlayer = mc.thePlayer ?: return

		chat(thePlayer, "\u00A73Your ping is \u00A7a${mc.netHandler.getPlayerInfo(thePlayer.uniqueID)?.responseTime ?: 0}ms\u00A73.")
	}
}
