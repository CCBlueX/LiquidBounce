/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.cantBoostUp
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.moveDirectionRadians
import net.ccbluex.liquidbounce.utils.timer.MSTimer

class TeleportCubeCraft : SpeedMode("TeleportCubeCraft")
{
    private val timer = MSTimer()

    override fun onMotion(eventState: EventState)
    {
    }

    override fun onUpdate()
    {
    }

    override fun onMove(event: MoveEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.cantBoostUp) return

        if (thePlayer.isMoving && thePlayer.onGround && timer.hasTimePassed(300L))
        {
            val func = functions

            val distance = Speed.cubecraftPortLengthValue.get()

            val dir = thePlayer.moveDirectionRadians
            event.x = (-func.sin(dir) * distance).toDouble()
            event.z = (func.cos(dir) * distance).toDouble()
            timer.reset()
        }
    }
}
