package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.rotation

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class InvalidPitchCheck : BotCheck("rotation.invalidPitch")
{
	override val isActive: Boolean
		get() = AntiBot.rotationInvalidPitchEnabledValue.get()

	private val invalidPitch = mutableSetOf<Int>()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean = if (AntiBot.rotationInvalidPitchKeepVLValue.get()) target.entityId in invalidPitch else (target.rotationPitch !in -90f..90f)

	override fun onEntityMove(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityPlayer, isTeleport: Boolean, newPos: WVec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
	{
		if (rotating)
		{
			val entityId = target.entityId
			if ((newPitch > 90.0F || newPitch < -90.0F) && entityId !in invalidPitch) invalidPitch.add(entityId)
		}
	}

	override fun clear()
	{
		invalidPitch.clear()
	}
}
