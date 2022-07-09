package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode

class NeruxVaceGlide : FlyMode("NeruxVace")
{
    private var ticks = 0

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        val onGround = thePlayer.onGround

        if (!onGround) ticks++

        if (ticks >= Fly.neruxVaceTicks.get() && !onGround)
        {
            ticks = 0
            thePlayer.motionY = 0.015
        }
    }
}
