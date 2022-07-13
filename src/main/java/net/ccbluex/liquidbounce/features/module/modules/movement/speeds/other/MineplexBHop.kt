/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.cantBoostUp
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.strafe
import kotlin.math.hypot

class MineplexBHop : SpeedMode("Mineplex-BHop")
{

    private var speed1 = 0f
    private var speed2 = 0f
    private var wfg = false
    private var fallDistance = 0f

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.cantBoostUp) return

        val speed = hypot(thePlayer.posX - thePlayer.prevPosX, thePlayer.posZ - thePlayer.prevPosZ).toFloat()

        if (thePlayer.isMoving && thePlayer.onGround)
        {
            thePlayer.motionY = 0.4052393

            wfg = true

            speed2 = speed1
            speed1 = 0f
        }
        else
        {
            if (wfg)
            {
                speed1 = (speed2 + (0.46532f * fallDistance.coerceAtMost(1f)))
                wfg = false
            }
            else speed1 = speed * 0.936f

            fallDistance = thePlayer.fallDistance
        }

        var minimum = 0f
        if (!wfg) minimum = 0.3999001f

        thePlayer.strafe(speed1.coerceIn(minimum, 2f))
    }

    override fun onMotion(eventState: EventState)
    {
    }

    override fun onMove(event: MoveEvent)
    {
    }

    override fun onDisable()
    {
        speed1 = 0f
        speed2 = 0f

        wfg = false

        fallDistance = 0f
    }
}
