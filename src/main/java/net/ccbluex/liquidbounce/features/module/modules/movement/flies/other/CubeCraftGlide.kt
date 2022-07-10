package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.cos
import net.ccbluex.liquidbounce.utils.extensions.sin
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.timer.TickTimer

class CubeCraftGlide : FlyMode("CubeCraft")
{
    private val cubecraftTeleportTickTimer = TickTimer()

    override fun onUpdate()
    {
        val timer = mc.timer

        timer.timerSpeed = 0.6f
        cubecraftTeleportTickTimer.update()
    }

    override fun onMove(event: MoveEvent)
    {
        val yaw = (mc.thePlayer ?: return).rotationYaw.toRadians

        if (cubecraftTeleportTickTimer.hasTimePassed(2))
        {
            event.x = -yaw.sin * 2.4
            event.z = yaw.cos * 2.4

            cubecraftTeleportTickTimer.reset()
        }
        else
        {
            event.x = -yaw.sin * 0.2
            event.z = yaw.cos * 0.2
        }
    }
}
