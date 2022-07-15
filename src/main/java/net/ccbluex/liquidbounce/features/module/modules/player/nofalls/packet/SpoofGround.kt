package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.NoFall
import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.minecraft.network.play.client.C03PacketPlayer

class SpoofGround : NoFallMode("SpoofGround")
{
    override fun onMovePacket(packet: C03PacketPlayer): Boolean
    {
        val thePlayer = mc.thePlayer ?: return false
        if (checkFallDistance(thePlayer))
        {
            if (noSpoof >= NoFall.noSpoofTicks.get())
            {
                noSpoof = 0
                return true
            }

            noSpoof++
        }

        return packet.onGround
    }
}
