/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.minecraft.network.play.client.C03PacketPlayer

object Hypixel : NoFallMode("Hypixel") {
    override fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer) {
            if (mc.thePlayer != null && mc.thePlayer.fallDistance > 1.5)
                event.packet.onGround = mc.thePlayer.ticksExisted % 2 == 0
        }
    }
}