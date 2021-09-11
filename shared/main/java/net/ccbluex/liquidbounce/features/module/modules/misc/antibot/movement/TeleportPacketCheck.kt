package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.movement

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import kotlin.math.pow
import kotlin.math.sqrt

class TeleportPacketCheck : BotCheck("move.teleportPacket")
{
	override val isActive: Boolean
		get() = AntiBot.teleportPacketEnabledValue.get()

	private val vl = mutableMapOf<Int, Int>()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean
	{
		val entityId = target.entityId
		return entityId in vl && (!AntiBot.teleportPacketVLEnabledValue.get() || (vl[entityId] ?: 0) >= AntiBot.teleportPacketVLLimitValue.get())
	}

	override fun onEntityMove(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityPlayer, isTeleport: Boolean, newPos: WVec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
	{
		val entityId = target.entityId

		if (!isTeleport) return

		val previousVL = vl[entityId] ?: 0

		val distSq = target.getDistanceSq(newPos.xCoord, newPos.yCoord, newPos.zCoord)
		if (distSq <= AntiBot.teleportPacketThresholdDistanceValue.get().pow(2))
		{
			if ((previousVL + 5) % 10 == 0) notification(target) { "Suspicious teleport: ${sqrt(distSq)} blocks" }
			vl[entityId] = previousVL + 2
		}
		else if (AntiBot.teleportPacketVLDecValue.get())
		{
			val currentVL = previousVL - 1
			if (currentVL <= 0) vl.remove(entityId) else vl[entityId] = currentVL
		}
	}

	override fun clear()
	{
		vl.clear()
	}
}
