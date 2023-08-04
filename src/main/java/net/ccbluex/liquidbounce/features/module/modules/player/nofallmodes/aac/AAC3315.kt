package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.network.play.client.C03PacketPlayer

object AAC3315 : NoFallMode("AAC3.3.15") {
    override fun onUpdate() {
        if (mc.thePlayer.fallDistance > 2) {
            if (!mc.isIntegratedServerRunning)
                PacketUtils.sendPacket(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        mc.thePlayer.posX,
                        Double.NaN,
                        mc.thePlayer.posZ,
                        false
                    )
                )

            mc.thePlayer.fallDistance = -9999f
        }
    }
}