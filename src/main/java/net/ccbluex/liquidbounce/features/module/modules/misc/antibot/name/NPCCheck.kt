package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.name

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class NPCCheck : BotCheck("name.npc")
{
	override val isActive: Boolean
		get() = AntiBot.npcValue.get()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer) = "\u00A78[NPC]" in target.displayName.formattedText
}
