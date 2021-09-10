package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.movement

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class AlwaysInRadiusCheck : BotCheck("move.alwaysInRadius")
{
	override val isActive: Boolean
		get() = AntiBot.alwaysInRadiusEnabledValue.get()

	private val outOfRadius = mutableSetOf<Int>()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean = target.entityId !in outOfRadius

	override fun onEntityMove(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityPlayer, isTeleport: Boolean, newPos: WVec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
	{
		val entityId = target.entityId
		if (entityId !in outOfRadius && thePlayer.getDistanceToEntity(target) > AntiBot.alwaysInRadiusRadiusValue.get()) outOfRadius.add(entityId)
	}

	override fun clear()
	{
		outOfRadius.clear()
	}
}
