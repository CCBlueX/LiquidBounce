/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.cantBoostUp
import net.ccbluex.liquidbounce.utils.extensions.isMoving

// Original author: 1337quip (wasd#9800)
class AAC4_4_0BHop : SpeedMode("AAC4.4.0-BHop")
{
    override fun onDisable()
    {
        mc.timer.timerSpeed = 1f
        mc.thePlayer?.speedInAir = 0.02F
    }

    override fun onMotion(eventState: EventState)
    {
    }

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isMoving || thePlayer.cantBoostUp) return

        // Check if player is on ground
        if (thePlayer.onGround) jump(thePlayer) //Set player to jump

        val timer = mc.timer

        // Check if player isn't on ground & Check player fall distance
        if (!thePlayer.onGround && thePlayer.fallDistance <= 0.1)
        {
            // Set player speed in air to 0.02
            thePlayer.speedInAir = 0.02F

            // Set player timer speed to 1.4 (1.5)
            timer.timerSpeed = 1.4F
        }

        // Check player fall distance
        if (thePlayer.fallDistance > 0.1 && thePlayer.fallDistance < 1.3)
        {
            // Set player speed in air to 0.0205
            thePlayer.speedInAir = 0.0205F
            // Set player timer speed to 0.65 (0.7)
            timer.timerSpeed = 0.65F
        }

        // Check player fall distance
        if (thePlayer.fallDistance >= 1.3)
        {
            // Reset player timer speed to 1
            timer.timerSpeed = 1F

            // Reset player speed in air to 0.02
            thePlayer.speedInAir = 0.02F
        }
    }

    override fun onMove(event: MoveEvent)
    {
    }
}
