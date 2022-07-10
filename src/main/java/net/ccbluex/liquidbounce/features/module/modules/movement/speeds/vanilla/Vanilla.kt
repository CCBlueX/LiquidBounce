package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.vanilla

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.cos
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.moveDirectionRadians
import net.ccbluex.liquidbounce.utils.extensions.sin

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
class Vanilla : SpeedMode("Vanilla")
{
    override fun onMotion(eventState: EventState)
    {
    }

    override fun onUpdate()
    {
    }

    override fun onMove(event: MoveEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.isMoving)
        {
            val moveSpeed = (LiquidBounce.moduleManager[Speed::class.java] as Speed).vanillaSpeedValue.get()
            val dir = thePlayer.moveDirectionRadians
            event.x = (-dir.sin * moveSpeed).toDouble()
            event.z = (dir.cos * moveSpeed).toDouble()
        }
    }
}
