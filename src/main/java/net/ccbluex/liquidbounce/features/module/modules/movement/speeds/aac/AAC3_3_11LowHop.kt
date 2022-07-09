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

class AAC3_3_11LowHop : SpeedMode("AAC3.3.11-LowHop") // Was AAC6BHop
{
    private var isFirst = false

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val timer = mc.timer

        timer.timerSpeed = 1f

        if (thePlayer.cantBoostUp) return

        if (thePlayer.isMoving)
        {
            if (thePlayer.onGround)
            {
                if (isFirst)
                {
                    thePlayer.strafe(0.15f)

                    thePlayer.onGround = false

                    thePlayer.motionY = 0.4
                    LiquidBounce.eventManager.callEvent(JumpEvent(0.4f))

                    isFirst = false
                    return
                }

                thePlayer.strafe(0.47458485f)

                thePlayer.motionY = 0.41
                LiquidBounce.eventManager.callEvent(JumpEvent(0.41f))
            }

            if (thePlayer.motionY < 0 && thePlayer.motionY > -0.2) timer.timerSpeed = (1.2f + thePlayer.motionY).toFloat()

            thePlayer.speedInAir = 0.022151f
        }
        else
        {
            isFirst = true

            thePlayer.zeroXZ()
        }
    }

    override fun onMotion(eventState: EventState)
    {
    }

    override fun onMove(event: MoveEvent)
    {
    }

    override fun onEnable()
    {
        isFirst = true
    }

    override fun onDisable()
    {
        mc.timer.timerSpeed = 1f
        mc.thePlayer?.speedInAir = 0.02f
    }
}
