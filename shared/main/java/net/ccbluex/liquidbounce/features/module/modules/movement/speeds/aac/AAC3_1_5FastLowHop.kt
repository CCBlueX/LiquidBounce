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
import net.ccbluex.liquidbounce.utils.extensions.cantBoostUp
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.zeroXZ

class AAC3_1_5FastLowHop : SpeedMode("AAC3.1.5-TimerLowHop") // Was AACLowHop2
{
    private var shouldLegitJump = true

    override fun onEnable()
    {
        shouldLegitJump = true

        mc.timer.timerSpeed = 1f
    }

    override fun onDisable()
    {
        mc.timer.timerSpeed = 1f
    }

    override fun onMotion(eventState: EventState)
    {
        if (eventState != EventState.PRE) return

        val thePlayer = mc.thePlayer ?: return
        val timer = mc.timer

        timer.timerSpeed = 1f

        if (thePlayer.cantBoostUp) return

        if (thePlayer.isMoving)
        {
            timer.timerSpeed = 1.09f

            if (thePlayer.onGround)
            {
                if (shouldLegitJump)
                {
                    jump(thePlayer)
                    shouldLegitJump = false
                    return
                }

                thePlayer.strafe(0.534f)

                thePlayer.motionY = 0.343
                LiquidBounce.eventManager.callEvent(JumpEvent(0.343f))
            }
        }
        else
        {
            shouldLegitJump = true

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
