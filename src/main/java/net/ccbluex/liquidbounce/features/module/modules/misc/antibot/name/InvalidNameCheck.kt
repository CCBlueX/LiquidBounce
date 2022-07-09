package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.name

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class InvalidNameCheck : BotCheck("name.invalidName")
{
	override val isActive: Boolean
		get() = AntiBot.invalidProfileNameValue.get()

	private val invalidProfileNameRegex = Regex("[^a-zA-Z0-9_]*")

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer) = invalidProfileNameRegex.containsMatchIn(target.gameProfile.name)
}
