package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object AAC3315 : NoFallMode("AAC3.3.15") {
    override fun onUpdate() {
        if (mc.isIntegratedServerRunning) return

        if (player.fallDistance > 2) {
            sendPacket(C04PacketPlayerPosition(player.posX, Double.NaN, player.posZ, false))

            player.fallDistance = -9999f
        }
    }
}