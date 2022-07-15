package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.minecraft.network.play.client.C03PacketPlayer

class NoGround : NoFallMode("NoGround")
{
    override fun onMovePacket(packet: C03PacketPlayer): Boolean = false
}
