package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.movement

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import kotlin.math.*

// TODO:  Movement direction check - 플레이어가 머리를 돌리는 것에 반응하여 그 정 반대 방향으로 움직이는지 검사
class MoveDirectionCheck : BotCheck("move.direction")
{
	override val isActive: Boolean
		get() = AntiBot.positionEnabledValue.get()

	private val positionVL = mutableMapOf<Int, Int>()
	private val positionConsistencyLastDistanceDelta = mutableMapOf<Int, MutableMap<Int, Double>>()
	private val positionConsistencyVL = mutableMapOf<Int, Int>()
	private val spawnPositionSuspects = mutableSetOf<Int>()

	override fun isBot(theWorld: IWorldClient, thePlayer: IEntity, target: IEntityPlayer): Boolean
	{
		val entityId = target.entityId
		return AntiBot.positionEnabledValue.get() && (positionVL[entityId] ?: 0) >= AntiBot.positionDeltaVLLimitValue.get() || AntiBot.positionDeltaConsistencyEnabledValue.get() && (positionConsistencyVL[entityId] ?: 0) >= AntiBot.positionDeltaConsistencyVLLimitValue.get()
	}

	override fun onEntityMove(theWorld: IWorldClient, thePlayer: IEntityPlayer, target: IEntityPlayer, isTeleport: Boolean, newPos: WVec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
	{
		val entityId = target.entityId

		val serverLocation = getPingCorrectionAppliedLocation(thePlayer)
		val isSuspectedForSpawnPosition = AntiBot.positionSpawnedPositionEnabledValue.get() && entityId in spawnPositionSuspects

		val serverPos = serverLocation.position
		val serverYaw = serverLocation.rotation.yaw

		var yawMovementScore = ceil(max(abs(getPingCorrectionAppliedLocation(thePlayer, 1).rotation.yaw - serverYaw), abs(getPingCorrectionAppliedLocation(thePlayer, 2).rotation.yaw - serverYaw)) / 5F).toInt()
		if (yawMovementScore <= 5) yawMovementScore = 0

		val yawRadians = WMathHelper.toRadians(serverYaw - 180.0F)

		// Position delta limit
		val positionDeltaLimitSq = AntiBot.positionDeltaThresholdValue.get().pow(2)
		val positionDeltaVLDec = AntiBot.positionDeltaVLDecValue.get()

		// Position delta consistency
		val positionRequiredDeltaToCheckConsistency = AntiBot.positionDeltaConsistencyRequiredDeltaToCheckValue.get()
		val positionDeltaConsistencyLimit = AntiBot.positionDeltaConsistencyConsistencyThresholdValue.get()
		val positionDeltaConsistencyVLDec = AntiBot.positionDeltaConsistencyVLDecValue.get()

		// Remove on caught
		val removeOnCaught = AntiBot.positionRemoveDetectedEnabledValue.get()
		val removeOnVL = AntiBot.positionRemoveDetectedVLValue.get()

		val speed = hypot(target.posX - newPos.xCoord, target.posZ - newPos.zCoord)

		for ((posIndex, back, y) in arrayOf(Triple(1, AntiBot.positionPosition1BackValue.get(), AntiBot.positionPosition1YValue.get()), Triple(2, AntiBot.positionPosition2BackValue.get(), AntiBot.positionPosition2YValue.get()), Triple(3, AntiBot.positionPosition3BackValue.get(), AntiBot.positionPosition3YValue.get()), Triple(4, AntiBot.positionPosition4BackValue.get(), AntiBot.positionPosition4YValue.get())))
		{
			val deltaX = newPos.xCoord - (serverPos.xCoord - functions.sin(yawRadians) * back)
			val deltaY = newPos.yCoord - (serverPos.yCoord + y)
			val deltaZ = newPos.zCoord - (serverPos.zCoord + functions.cos(yawRadians) * back)

			val distanceSq = deltaX * deltaX + deltaY * deltaY * deltaZ * deltaZ

			val previousVL = positionVL[entityId] ?: 0

			// Position Delta
			if (distanceSq <= positionDeltaLimitSq)
			{
				val baseScore = when
				{
					distanceSq <= positionDeltaLimitSq * 0.0005F -> 15
					distanceSq <= positionDeltaLimitSq * 0.02F -> 8
					distanceSq <= positionDeltaLimitSq * 0.05F -> 4
					distanceSq <= positionDeltaLimitSq * 0.1F -> 3
					distanceSq <= positionDeltaLimitSq * 0.2F -> 2
					else -> 1
				}
				val spawnPosScore = if (isSuspectedForSpawnPosition) if (speed >= 3) 100 else 10 else 0
				val speedScore = if (speed >= 2) ceil(speed * 2).toInt() else 0
				val extraScore = if (y >= 2) 10 else 0
				val vlIncrement = baseScore + yawMovementScore + spawnPosScore + speedScore + extraScore

				val newVL = previousVL + vlIncrement

				if (removeOnCaught && newVL > removeOnVL)
				{
					remove(theWorld, entityId, target.gameProfile.name, target.displayName.formattedText, "position")
					positionVL.remove(entityId)
				}
				else
				{
					if (((previousVL + 5) % 20 == 0) || (vlIncrement >= 5)) notification(target) { "Suspicious position: [posIndex: $posIndex, dist: ${StringUtils.DECIMALFORMAT_6.format(distanceSq)}, vl: (+$baseScore(base) +$yawMovementScore(yaw) +$spawnPosScore(spawnPos) +$speedScore(speed) +$extraScore(extra))]" }
					positionVL[entityId] = newVL
				}
			}
			else if (positionDeltaVLDec)
			{
				val currentVL = previousVL - 1
				if (currentVL <= 0) positionVL.remove(entityId) else positionVL[entityId] = currentVL
			}

			val prevConsistencyVL = positionConsistencyVL[entityId] ?: 0

			// Position Delta Consistency
			if (distanceSq <= positionRequiredDeltaToCheckConsistency)
			{
				val lastDistance = positionConsistencyLastDistanceDelta[entityId]

				if (lastDistance == null) positionConsistencyLastDistanceDelta[entityId] = LinkedHashMap(4)
				else
				{
					if (posIndex in lastDistance)
					{
						val consistency = abs(lastDistance[posIndex]!! - distanceSq)

						if (consistency <= positionDeltaConsistencyLimit)
						{
							val vlIncrement = when
							{
								consistency <= positionDeltaConsistencyLimit * 0.1F -> 10
								consistency <= positionDeltaConsistencyLimit * 0.25F -> 8
								consistency <= positionDeltaConsistencyLimit * 0.5F -> 5
								consistency <= positionDeltaConsistencyLimit * 0.75F -> 2
								else -> 1
							} + if (isSuspectedForSpawnPosition) 10 else 0

							if (((previousVL + 5) % 10 == 0 || vlIncrement >= 5)) notification(target) { "Suspicious position consistency: [posIndex: $posIndex, delta: ${StringUtils.DECIMALFORMAT_6.format(consistency)}, posVL: $previousVL, posConsistencyVL: $prevConsistencyVL)]" }
							positionConsistencyVL[entityId] = prevConsistencyVL + vlIncrement
						}
						else if (positionDeltaConsistencyVLDec)
						{
							val currentVL = prevConsistencyVL - 1
							if (currentVL <= 0) positionConsistencyVL.remove(entityId) else positionConsistencyVL[entityId] = currentVL
						}
					}
				}

				positionConsistencyLastDistanceDelta[entityId]!![posIndex] = distanceSq
			}
			else
			{
				val currentVL = prevConsistencyVL - 1
				if (currentVL <= 0) positionConsistencyVL.remove(entityId) else positionConsistencyVL[entityId] = currentVL
			}
		}
	}

	override fun onPacket(event: PacketEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val packet = event.packet

		if (classProvider.isSPacketSpawnPlayer(packet))
		{
			val playerSpawnPacket = packet.asSPacketSpawnPlayer()

			val entityId = playerSpawnPacket.entityID

			val entityX: Double = playerSpawnPacket.x.toDouble() / 32.0
			val entityZ: Double = playerSpawnPacket.z.toDouble() / 32.0

			val serverLocation = getPingCorrectionAppliedLocation(thePlayer)
			val serverPos = serverLocation.position

			if (hypot(serverPos.xCoord - entityX, serverPos.zCoord - entityZ) >= 6) spawnPositionSuspects.add(entityId)
		}
	}

	override fun onRender3D(event: Render3DEvent)
	{
		if (!AntiBot.positionMarkEnabledValue.get()) return

		val thePlayer = mc.thePlayer ?: return

		val partialTicks = event.partialTicks

		val serverLocation = getPingCorrectionAppliedLocation(thePlayer)
		val lastServerLocation = getPingCorrectionAppliedLocation(thePlayer, 1)

		val lastServerYaw = lastServerLocation.rotation.yaw

		val serverPos = serverLocation.position
		val lastServerPos = lastServerLocation.position

		val yaw = lastServerYaw + (serverLocation.rotation.yaw - lastServerYaw) * partialTicks

		val dir = WMathHelper.toRadians(yaw - 180.0F)

		val func = functions

		val sin = -func.sin(dir)
		val cos = func.cos(dir)

		val posX = lastServerPos.xCoord + (serverPos.xCoord - lastServerPos.xCoord) * partialTicks
		val posY = lastServerPos.yCoord + (serverPos.yCoord - lastServerPos.yCoord) * partialTicks
		val posZ = lastServerPos.zCoord + (serverPos.zCoord - lastServerPos.zCoord) * partialTicks

		val provider = classProvider

		val renderManager = mc.renderManager
		val renderPosX = renderManager.renderPosX
		val renderPosY = renderManager.renderPosY
		val renderPosZ = renderManager.renderPosZ

		val alpha = AntiBot.positionMarkAlphaValue.get()

		val deltaLimit = AntiBot.positionDeltaThresholdValue.get()

		val width = thePlayer.width + deltaLimit
		val height = thePlayer.height + deltaLimit

		val bb = provider.createAxisAlignedBB(-width - renderPosX, -renderPosY, -width - renderPosZ, width - renderPosX, height - renderPosY, width - renderPosZ)

		for ((back, y, color) in arrayOf(Triple(AntiBot.positionPosition1BackValue.get(), AntiBot.positionPosition1YValue.get(), 0xFF0000), Triple(AntiBot.positionPosition2BackValue.get(), AntiBot.positionPosition2YValue.get(), 0xFF8800), Triple(AntiBot.positionPosition3BackValue.get(), AntiBot.positionPosition3YValue.get(), 0x88FF00), Triple(AntiBot.positionPosition4BackValue.get(), AntiBot.positionPosition4YValue.get(), 0x00FF00))) RenderUtils.drawAxisAlignedBB(bb.offset(posX + sin * back, posY + y, posZ + cos * back), ColorUtils.applyAlphaChannel(color, alpha))
	}

	override fun clear()
	{
		positionVL.clear()
		positionConsistencyLastDistanceDelta.clear()
		positionConsistencyVL.clear()
		spawnPositionSuspects.clear()
	}
}
