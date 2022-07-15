package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.minecraft.network.play.client.C03PacketPlayer

// BetterNoFall - AAC2
class BetterAAC2 : NoFallMode("BetterAAC2")
{
    override fun onMovePacket(packet: C03PacketPlayer): Boolean
    {
        val thePlayer = mc.thePlayer ?: return packet.onGround

        if (checkFallDistance(thePlayer, 2.6f))
        {
            groundFallDistance += 2.9f
            thePlayer.motionY = 0.0
            return true
        }

        return packet.onGround
    }
}
