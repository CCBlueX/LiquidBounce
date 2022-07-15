package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.minecraft.network.play.client.C03PacketPlayer

class AAC3_3_15 : NoFallMode("AAC3.3.11")
{
    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        if (checkFallDistance(thePlayer))
        {
            if (!mc.isIntegratedServerRunning) mc.netHandler.networkManager.sendPacketWithoutEvent(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, Double.NaN, thePlayer.posZ, false))
            thePlayer.fallDistance = -9999.0F
        }
    }
}
