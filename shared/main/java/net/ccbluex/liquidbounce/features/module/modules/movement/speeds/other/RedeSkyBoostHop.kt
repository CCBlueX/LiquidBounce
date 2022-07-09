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

// Original author: RedeskySpeed v1 by noom#0681
// https://forums.ccbluex.net/topic/2110/very-fast-speed-for-redesky
class RedeSkyBoostHop : SpeedMode("RedeSky-BoostHop")
{
    private var delay = 0
    private var boost = 0F

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isMoving || thePlayer.cantBoostUp) return

        if (thePlayer.onGround)
        {
            thePlayer.jump()
            delay = 0
            boost = 0F
        }
        else
        {
            delay++

            if (delay % 3 == 0) boost += 0.08F

            if (boost <= 0.16) thePlayer.jumpMovementFactor = 0.22F - boost
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
        delay = 0
        boost = 0F
    }

    override fun onDisable()
    {
        delay = 0
        boost = 0F
    }
}
