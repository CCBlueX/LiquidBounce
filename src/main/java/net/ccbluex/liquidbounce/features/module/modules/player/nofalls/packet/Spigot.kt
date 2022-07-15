package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.packet

import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.minecraft.network.play.client.C03PacketPlayer

// BetterNoFall - Spigot
class Spigot : NoFallMode("Spigot")
{
    override fun onMovePacket(packet: C03PacketPlayer): Boolean
    {
        val theWorld = mc.theWorld ?: return packet.onGround
        val thePlayer = mc.thePlayer ?: return packet.onGround

        if (checkFallDistance(thePlayer, 3.2f) && theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, mc.thePlayer.motionY - 1, 0.0)).isNotEmpty()) packet.y -= 11
        return packet.onGround
    }
}
