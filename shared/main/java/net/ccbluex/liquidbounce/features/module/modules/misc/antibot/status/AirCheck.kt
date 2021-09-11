package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class AirCheck : BotCheck("status.air")
{
	override val isActive: Boolean
		get() = AntiBot.airValue.get()

	private val air = mutableSetOf<Int>()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean = target.entityId !in air

	override fun onEntityMove(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityPlayer, isTeleport: Boolean, newPos: WVec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
	{
		val entityId = target.entityId
		if (!onGround && entityId !in air) air.add(entityId)
	}

	override fun clear()
	{
		air.clear()
	}
}
