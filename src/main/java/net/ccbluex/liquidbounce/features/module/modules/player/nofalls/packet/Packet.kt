package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.NoFall
import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.minecraft.network.play.client.C03PacketPlayer

class Packet : NoFallMode("Packet")
{
    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        if (checkFallDistance(thePlayer))
        {
            if (noSpoof >= NoFall.noSpoofTicks.get())
            {
                mc.netHandler.networkManager.sendPacketWithoutEvent(C03PacketPlayer(true))
                noSpoof = 0
            }
            noSpoof++
        }
    }
}
