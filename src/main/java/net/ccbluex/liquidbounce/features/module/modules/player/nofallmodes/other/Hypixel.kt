package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.minecraft.network.play.client.C03PacketPlayer

object Hypixel : NoFallMode("Hypixel") {
    override fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer) {
            if (player != null && player.fallDistance > 1.5)
                event.packet.onGround = player.ticksExisted % 2 == 0
        }
    }
}