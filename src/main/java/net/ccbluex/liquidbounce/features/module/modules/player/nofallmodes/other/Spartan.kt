package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object Spartan : NoFallMode("Spartan") {
    private val spartanTimer = TickTimer()

    override fun onUpdate() {
        val player = player

        spartanTimer.update()
        if (player.fallDistance > 1.5 && spartanTimer.hasTimePassed(10)) {
            sendPackets(
                C04PacketPlayerPosition(player.posX, player.posY + 10, player.posZ, true),
                C04PacketPlayerPosition(player.posX, player.posY - 10, player.posZ, true)
            )
            spartanTimer.reset()
        }
    }
}