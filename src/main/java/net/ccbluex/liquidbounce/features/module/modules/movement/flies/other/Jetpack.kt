package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.multiply

class Jetpack : FlyMode("Jetpack")
{
    override val mark: Boolean
        get() = false

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        if (mc.gameSettings.keyBindJump.isKeyDown)
        {
            thePlayer.motionY += 0.15
            thePlayer.multiply(1.1)
        }
    }
}
