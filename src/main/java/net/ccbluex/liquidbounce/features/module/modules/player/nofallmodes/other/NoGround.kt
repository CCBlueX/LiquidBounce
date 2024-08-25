/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object NoGround : NoFallMode("NoGround") {
    override fun onPacket(event: PacketEvent) {
        if (event.packet is PlayerMoveC2SPacket)
            event.packet.onGround = false
    }
}