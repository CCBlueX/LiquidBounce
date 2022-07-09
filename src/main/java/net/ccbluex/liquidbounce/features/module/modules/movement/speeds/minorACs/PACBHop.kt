package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.minorACs

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.*

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
class PACBHop : SpeedMode("PAC-BHop")
{
    override fun onMotion(eventState: EventState)
    {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.cantBoostUp) return

        if (thePlayer.isMoving)
        {
            val moveSpeed = when (thePlayer.speedEffectAmplifier)
            {
                1 -> 0.40F // 0.31 +6 +6 +
                2 -> 0.48F // 0.37 - previous value
                3 -> 0.56F // 0.41
                4 -> 0.63F // 0.45
                5 -> 0.71F // 0.49
                6 -> 0.80F // 0.53
                else -> 0.33F
            }

            thePlayer.strafe(moveSpeed)

            if (thePlayer.onGround) jump(thePlayer)

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
