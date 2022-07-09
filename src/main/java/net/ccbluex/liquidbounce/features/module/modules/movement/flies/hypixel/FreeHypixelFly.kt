package net.ccbluex.liquidbounce.features.module.modules.movement.flies.hypixel

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import java.math.BigDecimal
import java.math.RoundingMode

class FreeHypixelFly : FlyMode("FreeHypixel")
{
    private val freeHypixelTimer = TickTimer()

    private var freeHypixelYaw = 0f
    private var freeHypixelPitch = 0f

    override fun onEnable()
    {
        val thePlayer = mc.thePlayer ?: return

        freeHypixelTimer.reset()
        thePlayer.setPositionAndUpdate(thePlayer.posX, thePlayer.posY + 0.42, thePlayer.posZ)
        freeHypixelYaw = thePlayer.rotationYaw
        freeHypixelPitch = thePlayer.rotationPitch
    }

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        if (freeHypixelTimer.hasTimePassed(10))
        {
            thePlayer.capabilities.isFlying = true
            return
        }

        // Watchdog Disabler Exploit
        RotationUtils.setTargetRotation(Rotation(freeHypixelYaw, freeHypixelPitch))

        thePlayer.motionY = 0.0
        thePlayer.motionZ = thePlayer.motionY
        thePlayer.motionX = thePlayer.motionZ

        if (Fly.startY == BigDecimal(thePlayer.posY).setScale(3, RoundingMode.HALF_DOWN).toDouble()) freeHypixelTimer.update()
    }

    override fun onMove(event: MoveEvent)
    {
        if (!freeHypixelTimer.hasTimePassed(10)) event.zero()
    }
}
