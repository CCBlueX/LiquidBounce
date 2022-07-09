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
import net.ccbluex.liquidbounce.utils.extensions.boost
import net.ccbluex.liquidbounce.utils.extensions.cantBoostUp
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.strafe

class NCPYPort : SpeedMode("NCPYPort")
{
    private var jumps = 0

    override fun onMotion(eventState: EventState)
    {
        if (eventState != EventState.PRE) return

        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isMoving || thePlayer.cantBoostUp || thePlayer.isInWater) return

        if (jumps >= 4 && thePlayer.onGround) jumps = 0

        if (thePlayer.onGround)
        {
            thePlayer.boost(0.2f)

            val jumpMotion = if (jumps <= 1) 0.42f else 0.4f
            thePlayer.motionY = jumpMotion.toDouble()
            LiquidBounce.eventManager.callEvent(JumpEvent(jumpMotion))

            jumps++
        }
        else if (jumps <= 1) thePlayer.motionY = -5.0

        thePlayer.strafe()
    }

    override fun onUpdate()
    {
    }

    override fun onMove(event: MoveEvent)
    {
    }
}
