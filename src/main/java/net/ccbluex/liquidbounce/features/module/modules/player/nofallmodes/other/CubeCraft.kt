package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer

object CubeCraft : NoFallMode("CubeCraft") {
    override fun onUpdate() {
        if (player.fallDistance > 2f) {
            player.onGround = false
            sendPacket(C03PacketPlayer(true))
        }
    }
}