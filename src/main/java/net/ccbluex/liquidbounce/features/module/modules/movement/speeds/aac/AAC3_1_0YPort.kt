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
import net.ccbluex.liquidbounce.utils.extensions.multiply

class AAC3_1_0YPort : SpeedMode("AAC3.1.0-YPort") // Was AACYPort
{
    override fun onMotion(eventState: EventState)
    {
        if (eventState != EventState.PRE) return

        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.cantBoostUp) return

        if (thePlayer.isMoving && !thePlayer.isSneaking)
        {
            if (thePlayer.onGround)
            {
                thePlayer.multiply(1.5893)

                thePlayer.motionY = 0.3425
                LiquidBounce.eventManager.callEvent(JumpEvent(0.3425f))
            }
            else thePlayer.motionY = -0.19
        }
    }

    override fun onUpdate()
    {
    }

    override fun onMove(event: MoveEvent)
    {
    }
}
