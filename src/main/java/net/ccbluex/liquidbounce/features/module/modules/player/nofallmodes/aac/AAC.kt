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
        val thePlayer = mc.player

        if (thePlayer.fallDistance > 2f) {
            sendPacket(PlayerMoveC2SPacket(true))
            currentState = 2
        } else if (currentState == 2 && thePlayer.fallDistance < 2) {
            thePlayer.velocityY = 0.1
            currentState = 3
            return
        }

        when (currentState) {
            3 -> {
                thePlayer.velocityY = 0.1
                currentState = 4
            }
            4 -> {
                thePlayer.velocityY = 0.1
                currentState = 5
            }
            5 -> {
                thePlayer.velocityY = 0.1
                currentState = 1
            }
        }
    }
}