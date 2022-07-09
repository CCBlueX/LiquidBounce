/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.cantBoostUp
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.zeroXZ

class NCPBHop : SpeedMode("NCPBHop")
{
    private var shouldLegitJump = false
    private var jumps = 0

    override fun onEnable()
    {
        shouldLegitJump = true

        jumps = 0

        mc.timer.timerSpeed = 1.0865f
    }

    override fun onDisable()
    {
        (mc.thePlayer ?: return).speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
    }

    override fun onMotion(eventState: EventState)
    {
    }

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.cantBoostUp) return

        if (thePlayer.isMoving)
        {
            if (thePlayer.onGround)
            {
                jump(thePlayer)

                if (shouldLegitJump) shouldLegitJump = false
                else
                {
                    val boostTicks = Speed.ncphopBoostTicks.get().coerceAtLeast(1)
                    if (jumps < boostTicks) thePlayer.speedInAir = 0.0223f else thePlayer.speedInAir = 0.02f

                    jumps++

                    if (jumps >= boostTicks + Speed.ncphopNoBoostTicks.get()) jumps = 0
                }
            }

            thePlayer.strafe()
        }
        else
        {
            shouldLegitJump = true
            jumps = 0

            thePlayer.zeroXZ()
        }
    }

    override fun onMove(event: MoveEvent)
    {
    }
}
