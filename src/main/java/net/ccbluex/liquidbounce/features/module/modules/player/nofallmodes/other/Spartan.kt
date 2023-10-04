package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object Spartan : NoFallMode("Spartan") {
    private val spartanTimer = TickTimer()

    override fun onUpdate() {
        val thePlayer = mc.thePlayer

        spartanTimer.update()
        if (thePlayer.fallDistance > 1.5 && spartanTimer.hasTimePassed(10)) {
            sendPackets(
                C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 10, thePlayer.posZ, true),
                C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY - 10, thePlayer.posZ, true)
            )
            spartanTimer.reset()
        }
    }
}