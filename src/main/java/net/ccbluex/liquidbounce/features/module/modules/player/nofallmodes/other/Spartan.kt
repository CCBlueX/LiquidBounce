package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object Spartan : NoFallMode("Spartan") {
    override fun onUpdate() {
        val spartanTimer = TickTimer()

        spartanTimer.update()
        if (mc.thePlayer.fallDistance > 1.5 && spartanTimer.hasTimePassed(10)) {
            sendPackets(
                C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY + 10,
                    mc.thePlayer.posZ,
                    true
                ),
                C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY - 10,
                    mc.thePlayer.posZ,
                    true
                )
            )
            spartanTimer.reset()
        }
    }
}