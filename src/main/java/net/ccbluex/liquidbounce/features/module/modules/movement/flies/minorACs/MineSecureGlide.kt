package net.ccbluex.liquidbounce.features.module.modules.movement.flies.minorACs

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.forward
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.zeroXZ
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

class MineSecureGlide : FlyMode("MineSecureGlide")
{
    private val mineSecureVClipTimer = MSTimer()

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val networkManager = mc.netHandler.networkManager
        val gameSettings = mc.gameSettings

        thePlayer.capabilities.isFlying = false

        if (!gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -0.01

        thePlayer.zeroXZ()

        thePlayer.strafe(Fly.baseSpeedValue.get())

        if (mineSecureVClipTimer.hasTimePassed(150) && gameSettings.keyBindJump.isKeyDown)
        {
            networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 5, thePlayer.posZ, false))
            networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(0.5, -1000.0, 0.5, false))

            thePlayer.forward(0.4)

            mineSecureVClipTimer.reset()
        }
    }
}
