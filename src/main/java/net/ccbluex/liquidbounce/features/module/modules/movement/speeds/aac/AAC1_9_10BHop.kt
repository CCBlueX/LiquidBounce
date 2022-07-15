/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.*

class AAC1_9_10BHop : SpeedMode("AAC1.9.10-BHop") // Was OldAACBHop
{
    override fun onMotion(eventState: EventState)
    {
        if (eventState != EventState.PRE) return

        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.cantBoostUp) return

        if (thePlayer.isMoving)
        {
            if (thePlayer.onGround)
            {
                thePlayer.strafe(0.56f)

                thePlayer.motionY = 0.41999998688697815
                LiquidBounce.eventManager.callEvent(JumpEvent(thePlayer.motionY.toFloat()))
            }
            else thePlayer.strafe(thePlayer.speed * if (thePlayer.fallDistance > 0.4f) 1.0f else 1.01f)
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
