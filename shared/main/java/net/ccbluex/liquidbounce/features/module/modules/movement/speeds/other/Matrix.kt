/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class Matrix : SpeedMode("Matrix") {
    override fun onEnable() {
        mc.timer.timerSpeed = 1.055f
    }
 
    override fun onDisable() {
        mc.thePlayer!!.speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
    }

    override fun onMotion() {}
    override fun onUpdate() {
        if (mc.thePlayer!!.isInWater) return
        if (MovementUtils.isMoving) {
            if (mc.thePlayer!!.onGround) {
                mc.thePlayer!!.jump()
                mc.thePlayer!!.speedInAir = 0.02055f
                mc.timer.timerSpeed = 1.055f
            }    
        } else {
            mc.thePlayer!!.motionX = 0.0
            mc.thePlayer!!.motionZ = 0.0
            mc.timer.timerSpeed = 1f    
        }
    }  
    override fun onMove(event: MoveEvent) {}
}
