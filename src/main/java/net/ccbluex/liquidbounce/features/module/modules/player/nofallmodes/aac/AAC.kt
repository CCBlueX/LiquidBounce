package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer

object AAC : NoFallMode("AAC") {

    private var currentState = 0

    override fun onUpdate() {
        val thePlayer = mc.thePlayer

        if (thePlayer.fallDistance > 2f) {
            sendPacket(C03PacketPlayer(true))
            currentState = 2
        } else if (currentState == 2 && thePlayer.fallDistance < 2) {
            thePlayer.motionY = 0.1
            currentState = 3
            return
        }

        when (currentState) {
            3 -> {
                thePlayer.motionY = 0.1
                currentState = 4
            }
            4 -> {
                thePlayer.motionY = 0.1
                currentState = 5
            }
            5 -> {
                thePlayer.motionY = 0.1
                currentState = 1
            }
        }
    }
}