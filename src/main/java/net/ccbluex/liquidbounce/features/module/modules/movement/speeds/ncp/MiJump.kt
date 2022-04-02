/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class MiJump : SpeedMode("MiJump") {
    override fun onMotion() {
        if (!MovementUtils.isMoving) return
        if (mc.thePlayer!!.onGround && !mc.thePlayer!!.movementInput.jump) {
            mc.thePlayer!!.motionY += 0.1
            val multiplier = 1.8
            mc.thePlayer!!.motionX *= multiplier
            mc.thePlayer!!.motionZ *= multiplier
            val currentSpeed = Math.sqrt(Math.pow(mc.thePlayer!!.motionX, 2.0) + Math.pow(mc.thePlayer!!.motionZ, 2.0))
            val maxSpeed = 0.66
            if (currentSpeed > maxSpeed) {
                mc.thePlayer!!.motionX = mc.thePlayer!!.motionX / currentSpeed * maxSpeed
                mc.thePlayer!!.motionZ = mc.thePlayer!!.motionZ / currentSpeed * maxSpeed
            }
        }
        MovementUtils.strafe()
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}