package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class HealthCheck : BotCheck("misc.health.over20")
{
	override val isActive: Boolean
		get() = AntiBot.healthValue.get()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer) = target.health > 20F
}
