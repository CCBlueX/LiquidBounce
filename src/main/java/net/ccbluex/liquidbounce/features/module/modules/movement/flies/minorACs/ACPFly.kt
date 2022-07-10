package net.ccbluex.liquidbounce.features.module.modules.movement.flies.minorACs

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.ccbluex.liquidbounce.utils.extensions.zeroXZ
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

class ACPFly : FlyMode("ACP") // ACP = AntiCheatPlus
{
    private val acpTickTimer = TickTimer()

    override fun onEnable()
    {
        val thePlayer = mc.thePlayer ?: return

        mc.netHandler.networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.4, thePlayer.posZ, thePlayer.onGround))
    }

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val gameSettings = mc.gameSettings

        thePlayer.motionY = 0.0

        if (gameSettings.keyBindJump.isKeyDown)
        {
            thePlayer.zeroXZ()
            thePlayer.motionY = 0.42
        }

        if (gameSettings.keyBindSneak.isKeyDown)
        {
            thePlayer.zeroXZ()
            thePlayer.motionY = -0.42
        }
    }

    override fun onMotion(eventState: EventState)
    {
        val thePlayer = mc.thePlayer ?: return

        acpTickTimer.update()
        if (acpTickTimer.hasTimePassed(4))
        {
            thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 1.0E-5, thePlayer.posZ)
            acpTickTimer.reset()
        }
    }
}
