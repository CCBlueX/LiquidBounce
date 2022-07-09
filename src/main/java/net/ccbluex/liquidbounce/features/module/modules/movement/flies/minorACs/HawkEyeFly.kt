package net.ccbluex.liquidbounce.features.module.modules.movement.flies.minorACs

import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode

class HawkEyeFly : FlyMode("HawkEye")
{
    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        thePlayer.motionY = if (thePlayer.motionY <= -0.42) 0.42 else -0.42
    }
}
