/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.cantBoostUp
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.multiply

class OnGround : SpeedMode("OnGround")
{
    override fun onMotion(eventState: EventState)
    {
        val thePlayer = mc.thePlayer ?: return

        if (eventState != EventState.PRE || !thePlayer.isMoving || thePlayer.fallDistance > 3.994 || thePlayer.cantBoostUp || thePlayer.isCollidedHorizontally) return

        val timer = mc.timer

        thePlayer.posY -= 0.3993000090122223
        thePlayer.motionY = -1000.0

        thePlayer.distanceWalkedModified = 44.0f

        timer.timerSpeed = 1f

        if (thePlayer.onGround)
        {
            thePlayer.posY += 0.3993000090122223
            thePlayer.multiply(1.590000033378601)

            thePlayer.motionY = 0.3993000090122223
            LiquidBounce.eventManager.callEvent(JumpEvent(0.3993000090122223f))

            timer.timerSpeed = 1.199f
        }
    }

    override fun onUpdate()
    {
    }

    override fun onMove(event: MoveEvent)
    {
    }
}
