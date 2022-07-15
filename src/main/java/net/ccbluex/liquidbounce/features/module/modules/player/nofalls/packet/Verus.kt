package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.minecraft.network.play.client.C03PacketPlayer

// BetterNoFall - Verus
class Verus : NoFallMode("Verus")
{
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
}
