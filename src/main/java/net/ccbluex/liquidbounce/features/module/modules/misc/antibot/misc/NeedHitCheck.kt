package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.misc

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class NeedHitCheck : BotCheck("misc.needHit")
{
	override val isActive: Boolean
		get() = AntiBot.needHitValue.get()

	private val hitted = mutableSetOf<Int>()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean = target.entityId !in hitted

	override fun onAttack(event: AttackEvent)
	{
		val entity = event.targetEntity
		if (entity != null && classProvider.isEntityLivingBase(entity) && entity.entityId !in hitted) hitted.add(entity.entityId)
	}

	override fun clear()
	{
		hitted.clear()
	}
}
