/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.minecraft.network.packet.c2s.play.C03PacketPlayer

object CubeCraft : NoFallMode("CubeCraft") {
    override fun onUpdate() {
        if (mc.player.fallDistance > 2f) {
            mc.player.onGround = false
            sendPacket(C03PacketPlayer(true))
        }
    }
}