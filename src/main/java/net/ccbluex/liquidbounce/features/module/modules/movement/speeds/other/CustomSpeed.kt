/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.cantBoostUp
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.zeroXZ

class CustomSpeed : SpeedMode("Custom")
{
    override fun onMotion(eventState: EventState)
    {
        if (eventState != EventState.PRE) return

        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.cantBoostUp) return

        if (thePlayer.isMoving)
        {
            mc.timer.timerSpeed = Speed.customTimerValue.get()
            when
            {
                thePlayer.onGround ->
                {
                    val customY = Speed.customYValue.get()

                    thePlayer.strafe(Speed.customSpeedValue.get())
                    thePlayer.motionY = customY.toDouble()
                    LiquidBounce.eventManager.callEvent(JumpEvent(customY))
                }

                Speed.customStrafeValue.get() -> thePlayer.strafe(Speed.customSpeedValue.get())
                else -> thePlayer.strafe()
            }
        }
        else thePlayer.zeroXZ()
    }

    override fun onEnable()
    {
        val thePlayer = mc.thePlayer ?: return

        if (Speed.customResetXZValue.get())
        {
            thePlayer.motionZ = 0.0
            thePlayer.motionX = thePlayer.motionZ
        }

        if (Speed.customResetYValue.get()) thePlayer.motionY = 0.0
    }

    override fun onDisable()
    {
        mc.timer.timerSpeed = 1f

        val thePlayer = mc.thePlayer ?: return

        if (Speed.customResetXZValue.get())
        {
            thePlayer.motionZ = 0.0
            thePlayer.motionX = thePlayer.motionZ
        }

        if (Speed.customResetYValue.get()) thePlayer.motionY = 0.0
    }

    override fun onUpdate()
    {
    }

    override fun onMove(event: MoveEvent)
    {
    }
}
