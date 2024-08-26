/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly

object AAC3315 : NoFallMode("AAC3.3.15") {
    override fun onUpdate() {
        val player = mc.player

        if (mc.isIntegratedServerRunning) return

        if (mc.player.fallDistance > 2) {
            sendPacket(PositionOnly(player.x, Double.NaN, player.z, false))

            player.fallDistance = -9999f
        }
    }
}