package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.minecraft.network.play.client.C03PacketPlayer

class AAC3_1_0 : NoFallMode("AAC3.1.0")
{
    private var aacTicks = 0

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        if (checkFallDistance(thePlayer))
        {
            mc.netHandler.networkManager.sendPacketWithoutEvent(C03PacketPlayer(true))
            aacTicks = 2
        }
        else if (aacTicks == 2 && thePlayer.fallDistance < 2)
        {
            thePlayer.motionY = 0.1
            aacTicks = 3
            return
        }

        when (aacTicks)
        {
            3 ->
            {
                thePlayer.motionY = 0.1
                aacTicks = 4
            }

            4 ->
            {
                thePlayer.motionY = 0.1
                aacTicks = 5
            }

            5 ->
            {
                thePlayer.motionY = 0.1
                aacTicks = 1
            }
        }
    }
}
