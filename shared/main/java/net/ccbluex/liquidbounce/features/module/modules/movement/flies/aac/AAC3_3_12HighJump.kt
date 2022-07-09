package net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.render.RenderUtils

class AAC3_3_12HighJump : FlyMode("AAC3.3.12-HighJump")
{
    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val timer = mc.timer

        if (thePlayer.posY < Fly.aac3_3_12YValue.get()) thePlayer.motionY = Fly.aac3_3_12MotionValue.get().toDouble()

        timer.timerSpeed = 1f

        if (mc.gameSettings.keyBindSneak.isKeyDown) // Help you to MLG
        {
            timer.timerSpeed = 0.2f
            mc.rightClickDelayTimer = 0
        }
    }

    override fun onRender3D(partialTicks: Float)
    {
        RenderUtils.drawPlatform(Fly.aac3_3_12YValue.get().toDouble(), 0x5A0000FF, 1.0)
    }
}
