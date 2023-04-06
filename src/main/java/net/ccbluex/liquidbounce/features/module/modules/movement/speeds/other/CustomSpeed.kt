/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe

class CustomSpeed : SpeedMode("Custom") {
    override fun onMotion() {
        if (isMoving) {
            mc.timer.timerSpeed = Speed.customTimerValue.get()
            when {
                mc.thePlayer.onGround -> {
                    strafe(Speed.customSpeedValue.get())
                    mc.thePlayer.motionY = Speed.customYValue.get().toDouble()
                }
                Speed.customStrafeValue.get() -> strafe(Speed.customSpeedValue.get())
                else -> strafe()
            }
        } else {
            mc.thePlayer.motionZ = 0.0
            mc.thePlayer.motionX = mc.thePlayer.motionZ
        }
    }

    override fun onEnable() {
        if (Speed.resetXZValue.get()) {
            mc.thePlayer.motionZ = 0.0
            mc.thePlayer.motionX = mc.thePlayer.motionZ
        }
        if (Speed.resetYValue.get()) mc.thePlayer.motionY = 0.0
        super.onEnable()
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        super.onDisable()
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}