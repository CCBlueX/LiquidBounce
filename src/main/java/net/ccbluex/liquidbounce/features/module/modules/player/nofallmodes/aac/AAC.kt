/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object AAC : NoFallMode("AAC") {

    private var currentState = 0

    override fun onUpdate() {
        val player = mc.player

        if (player.fallDistance > 2f) {
            sendPacket(PlayerMoveC2SPacket(true))
            currentState = 2
        } else if (currentState == 2 && player.fallDistance < 2) {
            player.velocityY = 0.1
            currentState = 3
            return
        }

        when (currentState) {
            3 -> {
                player.velocityY = 0.1
                currentState = 4
            }
            4 -> {
                player.velocityY = 0.1
                currentState = 5
            }
            5 -> {
                player.velocityY = 0.1
                currentState = 1
            }
        }
    }
}