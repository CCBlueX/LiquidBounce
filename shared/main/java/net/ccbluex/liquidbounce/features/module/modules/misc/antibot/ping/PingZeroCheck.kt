package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ping

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class PingZeroCheck : BotCheck("ping.zero")
{
	override val isActive: Boolean
		get() = AntiBot.pingZeroValue.get()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer) = mc.netHandler.getPlayerInfo(target.uniqueID)?.responseTime == 0
}
