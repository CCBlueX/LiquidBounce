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
		chat(mc.thePlayer, "\u00A73Your ping is \u00A7a${mc.netHandler.getPlayerInfo(mc.thePlayer!!.uniqueID)!!.responseTime}ms\u00A73.")
	}
}
