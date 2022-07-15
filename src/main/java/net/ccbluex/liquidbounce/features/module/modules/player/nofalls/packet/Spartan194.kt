package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.minecraft.network.play.client.C03PacketPlayer

class Spartan194 : NoFallMode("Spartan194")
{
    private val spartanTimer = TickTimer()

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val networkManager = mc.netHandler.networkManager

        val posX = thePlayer.posX
        val posY = thePlayer.posY
        val posZ = thePlayer.posZ

        spartanTimer.update()
        if (checkFallDistance(thePlayer) && spartanTimer.hasTimePassed(10))
        {
            networkManager.sendPacketWithoutEvent(C03PacketPlayer.C04PacketPlayerPosition(posX, posY + 10, posZ, true))
            networkManager.sendPacketWithoutEvent(C03PacketPlayer.C04PacketPlayerPosition(posX, posY - 10, posZ, true))
            spartanTimer.reset()
        }
    }
}
