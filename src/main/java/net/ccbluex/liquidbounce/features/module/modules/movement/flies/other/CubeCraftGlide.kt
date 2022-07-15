package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.forward
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
        val yaw = (mc.thePlayer ?: return).rotationYaw
        val length: Double
        if (cubecraftTeleportTickTimer.hasTimePassed(2))
        {
            length = 2.4
            cubecraftTeleportTickTimer.reset()
        }
        else length = 0.2

        event.forward(length, yaw)
    }
}
