package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import java.util.concurrent.LinkedBlockingQueue

// BetterNoFall - PacketAAC
class PacketAAC : NoFallMode("PacketAAC")
{
    private val packets = LinkedBlockingQueue<Packet<*>>()

    private var falling = false
    private var one = 0

    override fun onUpdate()
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.onGround) one = 0

        val cantNoFall = thePlayer.motionY >= 0 || thePlayer.isInWater || thePlayer.isInLava || thePlayer.isOnLadder || thePlayer.isInWeb

        if (theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset((thePlayer.motionX * 2), -0.8, (thePlayer.motionZ * 2))).isEmpty() && !cantNoFall)
        {
            // start blink
            falling = true
        }

        if (falling && (cantNoFall || thePlayer.onGround))
        {
            // stop blinking
            falling = false

            try
            {
                while (packets.isNotEmpty()) mc.netHandler.networkManager.sendPacketWithoutEvent(packets.take())
            }
            catch (e: Exception)
            {
                e.printStackTrace()
            }
        }
    }

    override fun onMovePacket(packet: C03PacketPlayer): Boolean
    {
        val thePlayer = mc.thePlayer ?: return packet.onGround

        if (checkFallDistance(thePlayer, 3.2f))
        {
            groundFallDistance += if (mc.thePlayer.motionY < -0.9) 3f else 3.2f
            thePlayer.motionY = 0.0
            return true
        }

        return packet.onGround
    }

    override fun onPacket(event: PacketEvent)
    {
        mc.thePlayer ?: return

        val packet = event.packet
        if (packet is C03PacketPlayer && falling)
        {
            event.cancelEvent()
            packets.add(packet)
        }
    }
}
