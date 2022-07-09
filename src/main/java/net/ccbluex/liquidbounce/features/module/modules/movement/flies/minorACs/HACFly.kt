package net.ccbluex.liquidbounce.features.module.modules.movement.flies.minorACs

import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.multiply

class HACFly : FlyMode("HAC") // HAC = HeirteirsAntiCheat
{
    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        thePlayer.multiply(0.8)
        thePlayer.motionY = if (thePlayer.motionY <= -0.42) 0.42 else -0.42
    }
}
