package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.minecraft.network.play.client.C03PacketPlayer

class CubeCraft : NoFallMode("CubeCraft")
{
    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        if (checkFallDistance(thePlayer))
        {
            thePlayer.onGround = false
            mc.netHandler.networkManager.sendPacketWithoutEvent(C03PacketPlayer(true))
        }
    }
}
