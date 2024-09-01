/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly

object Spartan : NoFallMode("Spartan") {
    private val spartanTimer = TickTimer()

    override fun onUpdate() {
        val player = mc.player

        spartanTimer.update()
        if (player.fallDistance > 1.5 && spartanTimer.hasTimePassed(10)) {
            sendPackets(
                PositionOnly(player.x, player.y + 10, player.z, true),
                PositionOnly(player.x, player.y - 10, player.z, true)
            )
            spartanTimer.reset()
        }
    }
}