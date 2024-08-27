/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.MovementUtils.serverOnGround
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly

object AAC3311 : NoFallMode("AAC3.3.11") {
    override fun onUpdate() {
        val player = mc.player

        if (player.fallDistance > 2) {
            player.stopXZ()

            sendPackets(
                PositionOnly(player.x, player.y - 10E-4, player.z, serverOnGround),
                PlayerMoveC2SPacket(true)
            )
        }
    }
}