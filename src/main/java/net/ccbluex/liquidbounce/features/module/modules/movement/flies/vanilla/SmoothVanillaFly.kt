package net.ccbluex.liquidbounce.features.module.modules.movement.flies.vanilla

import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode

class SmoothVanillaFly : FlyMode("SmoothVanilla")
{
    override val mark: Boolean
        get() = false

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        thePlayer.capabilities.isFlying = true

        handleVanillaKickBypass(mc.theWorld ?: return, thePlayer)
    }
}
