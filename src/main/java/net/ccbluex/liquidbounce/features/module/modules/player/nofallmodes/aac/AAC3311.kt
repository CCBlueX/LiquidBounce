package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.minecraft.network.play.client.C03PacketPlayer

object AAC3311 : NoFallMode("AAC3.3.11") {
    override fun onUpdate() {
        val thePlayer = mc.thePlayer

        if (thePlayer.fallDistance > 2) {
            thePlayer.motionZ = 0.0
            thePlayer.motionX = thePlayer.motionZ
            sendPackets(
                C03PacketPlayer.C04PacketPlayerPosition(
                    thePlayer.posX,
                    thePlayer.posY - 10E-4,
                    thePlayer.posZ,
                    thePlayer.onGround
                ),
                C03PacketPlayer(true)
            )
        }
    }
}