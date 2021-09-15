package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.rotation

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.RotationUtils

class YawMovementCheck : BotCheck("rotation.yaw")
{
	override val isActive: Boolean
		get() = AntiBot.rotationYawEnabledValue.get()

	private val previousYawMap = mutableMapOf<Int, Float>()
	private val yawMovement = mutableSetOf<Int>()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean = target.entityId !in yawMovement

	override fun onEntityMove(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityPlayer, isTeleport: Boolean, newPos: WVec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
	{
		if (rotating)
		{
			val entityId = target.entityId

			val prevPitch = previousYawMap.computeIfAbsent(entityId) { newYaw }

			if (RotationUtils.getAngleDifference(newYaw, prevPitch) > AntiBot.rotationYawThresholdValue.get() && entityId !in yawMovement) yawMovement.add(entityId)

			previousYawMap[entityId] = newYaw
		}
	}

	override fun clear()
	{
		previousYawMap.clear()
		yawMovement.clear()
	}
}
