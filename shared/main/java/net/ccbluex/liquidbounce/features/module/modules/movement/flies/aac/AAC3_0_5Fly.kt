package net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode

class AAC3_0_5Fly : FlyMode("AAC3.0.5")
{
    private var ticks = 0

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        if (ticks == 2) thePlayer.motionY = 0.1 else if (ticks > 2) ticks = 0
        if (Fly.aacFast.get()) if (thePlayer.movementInput.moveStrafe == 0.0f) thePlayer.jumpMovementFactor = 0.08f else thePlayer.jumpMovementFactor = 0f

        ticks++
    }
}
