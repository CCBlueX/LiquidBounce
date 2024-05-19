package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer

object AAC : NoFallMode("AAC") {

    private var currentState = 0

    override fun onUpdate() {
        val player = player

        if (player.fallDistance > 2f) {
            sendPacket(C03PacketPlayer(true))
            currentState = 2
        } else if (currentState == 2 && player.fallDistance < 2) {
            player.motionY = 0.1
            currentState = 3
            return
        }

        when (currentState) {
            3 -> {
                player.motionY = 0.1
                currentState = 4
            }
            4 -> {
                player.motionY = 0.1
                currentState = 5
            }
            5 -> {
                player.motionY = 0.1
                currentState = 1
            }
        }
    }
}