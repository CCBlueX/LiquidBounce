package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.movement

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import kotlin.math.pow

class WasMovedCheck : BotCheck("move.wasMoved")
{
	override val isActive: Boolean
		get() = AntiBot.wasMovedEnabledValue.get()

	private val spawnedPosition = mutableMapOf<Int, WVec3>()
	private val moved = mutableSetOf<Int>()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean = target.entityId in moved

	override fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (classProvider.isSPacketSpawnPlayer(packet))
		{
			val playerSpawnPacket = packet.asSPacketSpawnPlayer()

			spawnedPosition[playerSpawnPacket.entityID] = WVec3(playerSpawnPacket.x.toDouble() / 32.0, playerSpawnPacket.y.toDouble() / 32.0, playerSpawnPacket.z.toDouble() / 32.0)
		}
	}

	override fun onEntityMove(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityPlayer, isTeleport: Boolean, newPos: WVec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
	{
		val entityId = target.entityId
		val spawnedPosition = spawnedPosition[entityId] ?: newPos

		val distance = (spawnedPosition.xCoord - newPos.xCoord).pow(2) + (spawnedPosition.yCoord - newPos.yCoord).pow(2) + (spawnedPosition.zCoord - newPos.zCoord).pow(2)
		if (distance >= AntiBot.wasMovedThresholdDistanceValue.get()) moved += entityId

		if (entityId !in this.spawnedPosition) this.spawnedPosition[entityId] = spawnedPosition
	}

	override fun clear()
	{
		spawnedPosition.clear()
		moved.clear()
	}
}
