package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.utils.extensions.zeroXZ
import net.minecraft.network.play.client.C03PacketPlayer

class ACP : NoFallMode("ACP")
{
    override fun onMovePacket(packet: C03PacketPlayer): Boolean
    {
        val thePlayer = mc.thePlayer ?: return false
        if (checkFallDistance(thePlayer))
        {
            thePlayer.zeroXZ()
            return true
        }

        return packet.onGround
    }
}
