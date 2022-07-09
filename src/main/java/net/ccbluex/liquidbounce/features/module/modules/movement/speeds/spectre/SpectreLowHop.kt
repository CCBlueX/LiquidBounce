/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spectre

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.cantBoostUp
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.zeroXZ

class SpectreLowHop : SpeedMode("Spectre-LowHop")
{
    override fun onMotion(eventState: EventState)
    {
        if (eventState != EventState.PRE) return

        val thePlayer = mc.thePlayer ?: return
        if (thePlayer.movementInput.jump) return

        if (thePlayer.cantBoostUp) return

        if (thePlayer.isMoving)
        {
            if (thePlayer.onGround)
            {
                thePlayer.strafe(1.1f)
                thePlayer.motionY = 0.15
            }

            thePlayer.strafe()
        }
        else thePlayer.zeroXZ()
    }

    override fun onUpdate()
    {
    }

    override fun onMove(event: MoveEvent)
    {
    }
}
