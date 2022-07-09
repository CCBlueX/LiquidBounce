package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.position

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.Vec3
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import kotlin.math.*

class PositionCheck : BotCheck("position.position")
{
    override val isActive: Boolean
        get() = AntiBot.positionEnabledValue.get()

    private val positionVL = mutableMapOf<Int, Int>()
    private val positionDetectionCount = mutableMapOf<Int, Int>()

    private val positionConsistencyLastDistanceDelta = mutableMapOf<Int, MutableMap<Int, Double>>()
    private val positionConsistencyVL = mutableMapOf<Int, Int>()
    private val positionConsistencyDetectionCount = mutableMapOf<Int, Int>()

    private val spawnPositionSuspects = mutableSetOf<Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean
    {
        val entityId = target.entityId
        return (positionVL[entityId] ?: 0) >= AntiBot.positionDeltaVLLimitValue.get() || AntiBot.positionDeltaConsistencyEnabledValue.get() && (positionConsistencyVL[entityId] ?: 0) >= AntiBot.positionDeltaConsistencyVLLimitValue.get()
    }

    override fun onEntityMove(theWorld: WorldClient, thePlayer: EntityPlayer, target: EntityPlayer, isTeleport: Boolean, newPos: Vec3, rotating: Boolean, newYaw: Float, newPitch: Float, onGround: Boolean)
    {
        val entityId = target.entityId

        val positionDeltaLimitSq = AntiBot.positionDeltaThresholdValue.get().pow(2)
        val positionDeltaVLDec = AntiBot.positionDeltaVLDecValue.get()
        val positionRequiredDeltaToCheckConsistency = AntiBot.positionDeltaConsistencyRequiredDeltaToCheckValue.get()
        val positionDeltaConsistencyLimit = AntiBot.positionDeltaConsistencyConsistencyThresholdValue.get()
        val positionDeltaConsistencyVLDec = AntiBot.positionDeltaConsistencyVLDecValue.get()
        val isSuspectedForSpawnPosition = AntiBot.positionSpawnedPositionEnabledValue.get() && entityId in spawnPositionSuspects
        val remove = AntiBot.positionRemoveDetectedEnabledValue.get()
        val removeVL = AntiBot.positionRemoveDetectedVLValue.get()

        val serverLocation = getPingCorrectionAppliedLocation(thePlayer)
        val serverPos = serverLocation.position
        val serverYaw = serverLocation.rotation.yaw
        val dir = serverYaw - 180.0F.toRadians

        val moveSpeed = hypot(target.posX - newPos.xCoord, target.posZ - newPos.zCoord)

        for ((posIndex, back, y) in arrayOf(Triple(1, AntiBot.positionPosition1BackValue.get(), AntiBot.positionPosition1YValue.get()), Triple(2, AntiBot.positionPosition2BackValue.get(), AntiBot.positionPosition2YValue.get()), Triple(3, AntiBot.positionPosition3BackValue.get(), AntiBot.positionPosition3YValue.get()), Triple(4, AntiBot.positionPosition4BackValue.get(), AntiBot.positionPosition4YValue.get())))
        {
            val distanceSq = (newPos.xCoord - (serverPos.xCoord - functions.sin(dir) * back)).pow(2) + (newPos.yCoord - (serverPos.yCoord + y)).pow(2) * (newPos.zCoord - (serverPos.zCoord + functions.cos(dir) * back)).pow(2)

            val previousVL = positionVL[entityId] ?: 0

            if (distanceSq <= positionDeltaLimitSq)
            {
                // Suspected
                val baseScore = when
                {
                    distanceSq <= positionDeltaLimitSq * 0.0005F -> 15
                    distanceSq <= positionDeltaLimitSq * 0.02F -> 8
                    distanceSq <= positionDeltaLimitSq * 0.05F -> 4
                    distanceSq <= positionDeltaLimitSq * 0.1F -> 3
                    distanceSq <= positionDeltaLimitSq * 0.2F -> 2
                    else -> 1
                }

                var yawMovementScore = ceil(max(abs(getPingCorrectionAppliedLocation(thePlayer, 1).rotation.yaw - serverYaw), abs(getPingCorrectionAppliedLocation(thePlayer, 2).rotation.yaw - serverYaw)) / 5F).toInt()
                if (yawMovementScore <= 5) yawMovementScore = 0

                val spawnPosScore = if (isSuspectedForSpawnPosition) if (moveSpeed >= 3) 100 else 10 else 0
                val speedScore = if (moveSpeed >= 2) ceil(moveSpeed * 2).toInt() else 0
                val extraScore = if (y >= 2) 10 else 0
                val totalScore = baseScore + yawMovementScore + spawnPosScore + speedScore + extraScore

                val newVL = previousVL + totalScore

                if (remove && newVL > removeVL)
                {
                    remove(theWorld, entityId, target.gameProfile.name, target.displayName.formattedText)
                    positionVL.remove(entityId)
                }
                else
                {
                    var detectionCount = (positionDetectionCount[entityId] ?: 0) + 1

                    if (detectionCount >= 5)
                    {
                        notification(target) { arrayOf("reason=expect", "posIndex=$posIndex", "delta=${StringUtils.DECIMALFORMAT_6.format(distanceSq)}", "vl=(+$baseScore(base) +$yawMovementScore(yaw) +$spawnPosScore(spawnPos) +$speedScore(speed) +$extraScore(extra))") }
                        detectionCount = 0
                    }

                    positionVL[entityId] = newVL
                    positionDetectionCount[entityId] = detectionCount
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
                val lastDistance = positionConsistencyLastDistanceDelta.computeIfAbsent(entityId) { hashMapOf() }

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

                        var detectionCount = (positionConsistencyDetectionCount[entityId] ?: 0) + 1

                        if (detectionCount >= 5)
                        {
                            notification(target) { arrayOf("reason=consistency", "posIndex=$posIndex", "delta=${StringUtils.DECIMALFORMAT_6.format(consistency)}", "posVL=$previousVL", "posConsistencyVL=$prevConsistencyVL") }
                            detectionCount = 0
                        }

                        positionConsistencyVL[entityId] = prevConsistencyVL + vlIncrement
                        positionConsistencyDetectionCount[entityId] = detectionCount
                    }
                    else if (positionDeltaConsistencyVLDec)
                    {
                        val currentVL = prevConsistencyVL - 1
                        if (currentVL <= 0) positionConsistencyVL.remove(entityId) else positionConsistencyVL[entityId] = currentVL
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

        if (packet is SPacketSpawnPlayer)
        {
            val playerSpawnPacket = packet.asSPacketSpawnPlayer()

            val entityId = playerSpawnPacket.entityID

            val entityX: Double = playerSpawnPacket.x.toDouble() / 32.0
            val entityZ: Double = playerSpawnPacket.z.toDouble() / 32.0

            val serverPos = getPingCorrectionAppliedLocation(thePlayer).position
            if (hypot(serverPos.xCoord - entityX, serverPos.zCoord - entityZ) >= 6) spawnPositionSuspects.add(entityId)
        }
    }

    override fun onRender3D(event: Render3DEvent)
    {
        if (!AntiBot.positionMarkEnabledValue.get()) return

        val thePlayer = mc.thePlayer ?: return

        val partialTicks = event.partialTicks

        val alpha = AntiBot.positionMarkAlphaValue.get()
        val deltaLimit = AntiBot.positionDeltaThresholdValue.get()

        val serverLocation = getPingCorrectionAppliedLocation(thePlayer)
        val lastServerLocation = getPingCorrectionAppliedLocation(thePlayer, 1)

        val serverPos = serverLocation.position
        val lastServerPos = lastServerLocation.position
        val interpolatedX = lastServerPos.xCoord + (serverPos.xCoord - lastServerPos.xCoord) * partialTicks
        val interpolatedY = lastServerPos.yCoord + (serverPos.yCoord - lastServerPos.yCoord) * partialTicks
        val interpolatedZ = lastServerPos.zCoord + (serverPos.zCoord - lastServerPos.zCoord) * partialTicks

        val lastServerYaw = lastServerLocation.rotation.yaw
        val dir = lastServerYaw + (serverLocation.rotation.yaw - lastServerYaw) * partialTicks - 180.0F.toRadians
        val sin = -functions.sin(dir)
        val cos = functions.cos(dir)

        val renderManager = mc.renderManager
        val renderPosX = renderManager.renderPosX
        val renderPosY = renderManager.renderPosY
        val renderPosZ = renderManager.renderPosZ

        val width = thePlayer.width + deltaLimit
        val height = thePlayer.height + deltaLimit

        val bb = AxisAlignedBB(-width - renderPosX, -renderPosY, -width - renderPosZ, width - renderPosX, height - renderPosY, width - renderPosZ)

        for ((back, y, color) in arrayOf(Triple(AntiBot.positionPosition1BackValue.get(), AntiBot.positionPosition1YValue.get(), 0xFF0000), Triple(AntiBot.positionPosition2BackValue.get(), AntiBot.positionPosition2YValue.get(), 0xFF8800), Triple(AntiBot.positionPosition3BackValue.get(), AntiBot.positionPosition3YValue.get(), 0x88FF00), Triple(AntiBot.positionPosition4BackValue.get(), AntiBot.positionPosition4YValue.get(), 0x00FF00))) RenderUtils.drawAxisAlignedBB(bb.offset(interpolatedX + sin * back, interpolatedY + y, interpolatedZ + cos * back), ColorUtils.applyAlphaChannel(color, alpha))
    }

    override fun clear()
    {
        positionVL.clear()
        positionDetectionCount.clear()

        positionConsistencyLastDistanceDelta.clear()
        positionConsistencyVL.clear()
        positionConsistencyDetectionCount.clear()

        spawnPositionSuspects.clear()
    }
}
