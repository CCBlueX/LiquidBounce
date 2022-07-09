package net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.render.RenderUtils

class AAC1_9_10Fly : FlyMode("AAC1.9.10")
{
    override val shouldDisableNoFall: Boolean
        get() = true

    private var jump = 0.0

    override fun onEnable()
    {
        jump = -3.8
    }

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val gameSettings = mc.gameSettings

        val posY = thePlayer.posY

        if (gameSettings.keyBindJump.isKeyDown) jump += 0.2
        if (gameSettings.keyBindSneak.isKeyDown) jump -= 0.2

        if (Fly.startY + jump > posY)
        {
            mc.netHandler.networkManager.sendPacketWithoutEvent(CPacketPlayer(true))
            thePlayer.motionY = 0.8
            thePlayer.strafe(Fly.aacSpeedValue.get())
        }

        thePlayer.strafe()
    }

    override fun onRender3D(partialTicks: Float)
    {
        RenderUtils.drawPlatform(Fly.startY + jump, 0x5A0000FF, 1.0)
    }
}
