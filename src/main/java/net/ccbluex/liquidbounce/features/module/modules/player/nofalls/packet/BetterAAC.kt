package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.minecraft.network.play.client.C03PacketPlayer

// BetterNoFall - AAC
class BetterAAC : NoFallMode("BetterAAC")
{
    private var one = 0

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.onGround) one = 0
    }

    override fun onMovePacket(packet: C03PacketPlayer): Boolean
    {
        val theWorld = mc.theWorld ?: return packet.onGround
        val thePlayer = mc.thePlayer ?: return packet.onGround

        if (checkFallDistance(thePlayer) && !thePlayer.onGround && theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, thePlayer.motionY - 1, 0.0)).isNotEmpty() && one < 7)
        {
            one += 1
            packet.y -= if (mc.thePlayer.motionY <= -1) 2.0 else thePlayer.motionY - 1.0
        }

        return packet.onGround
    }
}
