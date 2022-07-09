package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.cantBoostUp
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.strafe
import kotlin.math.round

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
class MineplexBHop2 : SpeedMode("Mineplex-BHop2")
{
    var mineplex = 0
    var stage = 0

    override fun onMotion(eventState: EventState)
    {
    }

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val timer = mc.timer

        if (thePlayer.cantBoostUp) return

        val move = thePlayer.isMoving

        var moveSpeed = 0.15f

        if (thePlayer.isCollidedHorizontally || !move) mineplex = -2

        if (thePlayer.onGround && move)
        {
            stage = 0

            thePlayer.motionY = 0.42

            if (mineplex < 0) mineplex++
            if (thePlayer.posY != round(thePlayer.posY)) mineplex = -1

            timer.timerSpeed = 2.001f
        }
        else
        {
            if (timer.timerSpeed == 2.001f) timer.timerSpeed = 1.0F

            moveSpeed = (0.62f - stage / 300.0f + mineplex * 0.2f).coerceAtLeast(0f)

            stage++
        }

        thePlayer.strafe(moveSpeed)
    }

    override fun onMove(event: MoveEvent)
    {
    }

    fun onPacket(event: PacketEvent)
    {
        if (classProvider.isSPacketPlayerPosLook(event.packet))
        {
            mineplex = -2
            stage = 0
        }
    }

    override fun onEnable()
    {
        mineplex = -2
        stage = 0
    }
}
