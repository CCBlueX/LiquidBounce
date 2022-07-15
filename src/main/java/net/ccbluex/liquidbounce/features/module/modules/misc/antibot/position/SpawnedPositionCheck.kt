package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.position

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck
import net.ccbluex.liquidbounce.utils.extensions.applyForward
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S0CPacketSpawnPlayer
import net.minecraft.util.AxisAlignedBB
import kotlin.math.pow
import kotlin.math.sqrt

class SpawnedPositionCheck : BotCheck("position.spawnedPosition")
{
    override val isActive: Boolean
        get() = AntiBot.positionSpawnedPositionEnabledValue.get()

    private val spawnPosition = mutableSetOf<Int>()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean = target.entityId in spawnPosition

    override fun onPacket(event: PacketEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        val packet = event.packet

        if (packet is S0CPacketSpawnPlayer)
        {

            val entityId = packet.entityID

            val entityX: Double = packet.x.toDouble() / 32.0
            val entityY: Double = packet.y.toDouble() / 32.0
            val entityZ: Double = packet.z.toDouble() / 32.0

            val serverLocation = getPingCorrectionAppliedLocation(thePlayer)

            val serverPos = serverLocation.position
            val serverYaw = serverLocation.rotation.yaw

            val deltaLimit = AntiBot.positionSpawnedPositionDeltaThresholdValue.get().pow(2)

            for ((posIndex, back, y) in arrayOf(Triple(1, AntiBot.positionSpawnedPositionPosition1BackValue.get(), AntiBot.positionSpawnedPositionPosition1YValue.get()), Triple(2, AntiBot.positionPosition2BackValue.get(), AntiBot.positionSpawnedPositionPosition2YValue.get())))
            {
                var (expectDeltaX, expectDeltaZ) = (serverPos.xCoord to serverPos.zCoord).applyForward(back, serverYaw - 180.0F)
                expectDeltaX -= entityX
                val expectDeltaY = serverPos.yCoord + y - entityY
                expectDeltaZ -= entityZ

                val delta = expectDeltaX * expectDeltaX + expectDeltaY * expectDeltaY + expectDeltaZ * expectDeltaZ

                // Position Delta
                if (delta <= deltaLimit)
                {
                    notification { arrayOf("posIndex=$posIndex", "delta=${StringUtils.DECIMALFORMAT_6.format(sqrt(delta))}") }
                    spawnPosition.add(entityId)
                }
            }
        }
    }

    override fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
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

        val posX = lastServerPos.xCoord + (serverPos.xCoord - lastServerPos.xCoord) * partialTicks
        val posY = lastServerPos.yCoord + (serverPos.yCoord - lastServerPos.yCoord) * partialTicks
        val posZ = lastServerPos.zCoord + (serverPos.zCoord - lastServerPos.zCoord) * partialTicks

        val renderManager = mc.renderManager
        val renderPosX = renderManager.renderPosX
        val renderPosY = renderManager.renderPosY
        val renderPosZ = renderManager.renderPosZ

        val alpha = AntiBot.positionMarkAlphaValue.get()

        val deltaLimit = AntiBot.positionSpawnedPositionDeltaThresholdValue.get()

        val width = thePlayer.width + deltaLimit
        val height = thePlayer.height + deltaLimit

        val bb = AxisAlignedBB(-width - renderPosX, -renderPosY, -width - renderPosZ, width - renderPosX, height - renderPosY, width - renderPosZ)

        for ((back, y, color) in arrayOf(Triple(AntiBot.positionSpawnedPositionPosition1BackValue.get(), AntiBot.positionSpawnedPositionPosition1YValue.get(), 0x0088FF), Triple(AntiBot.positionSpawnedPositionPosition2BackValue.get(), AntiBot.positionSpawnedPositionPosition2YValue.get(), 0x0000FF)))
        {
            val (backX, backZ) = (posX to posZ).applyForward(back, yaw - 180.0F)
            RenderUtils.drawAxisAlignedBB(bb.offset(backX, posY + y, backZ), ColorUtils.applyAlphaChannel(color, alpha))
        }
    }

    override fun clear()
    {
        spawnPosition.clear()
    }
}
