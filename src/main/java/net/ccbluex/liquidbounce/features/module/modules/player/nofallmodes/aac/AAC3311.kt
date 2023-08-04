package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.network.play.client.C03PacketPlayer

object AAC3311 : NoFallMode("AAC3.3.11") {
    override fun onUpdate() {
        if (mc.thePlayer.fallDistance > 2) {
            mc.thePlayer.motionZ = 0.0
            mc.thePlayer.motionX = mc.thePlayer.motionZ
            PacketUtils.sendPackets(
                C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY - 10E-4,
                    mc.thePlayer.posZ,
                    mc.thePlayer.onGround
                ),
                C03PacketPlayer(true)
            )
        }
    }
}