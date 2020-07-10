/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class CustomSpeed : SpeedMode("Custom") {
    override fun onMotion() {
        if (MovementUtils.isMoving) {
            val speed = LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed? ?: return
            mc.timer.timerSpeed = speed.customTimerValue.get()
            when {
                mc.thePlayer!!.onGround -> {
                    MovementUtils.strafe(speed.customSpeedValue.get())
                    mc.thePlayer!!.motionY = speed.customYValue.get().toDouble()
                }
                speed.customStrafeValue.get() -> MovementUtils.strafe(speed.customSpeedValue.get())
                else -> MovementUtils.strafe()
            }
        } else {
            mc.thePlayer!!.motionZ = 0.0
            mc.thePlayer!!.motionX = mc.thePlayer!!.motionZ
        }
    }

    override fun onEnable() {
        val speed = LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed? ?: return
        if (speed.resetXZValue.get()) {
            mc.thePlayer!!.motionZ = 0.0
            mc.thePlayer!!.motionX = mc.thePlayer!!.motionZ
        }
        if (speed.resetYValue.get()) mc.thePlayer!!.motionY = 0.0
        super.onEnable()
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        super.onDisable()
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}