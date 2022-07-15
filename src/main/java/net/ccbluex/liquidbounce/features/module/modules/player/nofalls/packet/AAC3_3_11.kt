package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.minecraft.network.play.client.C03PacketPlayer

class AAC3_3_11 : NoFallMode("AAC3.3.11")
{
    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val networkManager = mc.netHandler.networkManager
        if (checkFallDistance(thePlayer))
        {
            thePlayer.motionZ = 0.0
            thePlayer.motionX = thePlayer.motionZ
            networkManager.sendPacketWithoutEvent(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY - 10E-4, thePlayer.posZ, thePlayer.onGround))
            networkManager.sendPacketWithoutEvent(C03PacketPlayer(true))
        }
    }
}
