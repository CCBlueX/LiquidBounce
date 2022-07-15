/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.*

class SlowHop : SpeedMode("SlowHop")
{
    private var requiredLegitCount = 2

    override fun onEnable()
    {
        requiredLegitCount = 2
    }

    override fun onMotion(eventState: EventState)
    {
        if (eventState != EventState.PRE) return

        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.cantBoostUp) return

        if (thePlayer.isMoving)
        {
            if (thePlayer.onGround)
            {
                jump(thePlayer)

                if (requiredLegitCount > 0) requiredLegitCount--
            }
            else if (requiredLegitCount <= 0) thePlayer.strafe(thePlayer.speed * Speed.slowHopMultiplierValue.get())
        }
        else
        {
            requiredLegitCount = 2

            thePlayer.zeroXZ()
        }
    }

    override fun onUpdate()
    {
    }

    override fun onMove(event: MoveEvent)
    {
    }
}
