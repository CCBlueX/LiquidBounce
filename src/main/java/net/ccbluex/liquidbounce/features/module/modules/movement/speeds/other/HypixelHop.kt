/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.*

class HypixelHop : SpeedMode("HypixelHop")
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
                thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 9.1314E-4, thePlayer.posZ) // #344

                jump(thePlayer)

                val speed = thePlayer.speed_f
                thePlayer.strafe((if (speed < 0.56f) speed * 1.045f else 0.56f) * (1.0F + 0.13f * thePlayer.speedEffectAmplifier))

                return
            }
            else if (thePlayer.motionY < 0.2) thePlayer.motionY -= 0.02

            thePlayer.strafe(thePlayer.speed_f * 1.01889f)
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
