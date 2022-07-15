package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.minecraft.network.play.client.C03PacketPlayer

class Hypixel : NoFallMode("ACP")
{
    override fun onMovePacket(packet: C03PacketPlayer): Boolean
    {
        val thePlayer = mc.thePlayer ?: return false
        return if (checkFallDistance(thePlayer)) thePlayer.ticksExisted % 2 == 0 else packet.onGround
    }
}
