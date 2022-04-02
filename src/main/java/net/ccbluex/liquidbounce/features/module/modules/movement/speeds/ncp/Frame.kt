/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.TickTimer

class Frame : SpeedMode("Frame") {
    private var motionTicks = 0
    private var move = false
    private val tickTimer = TickTimer()
    override fun onMotion() {
        if (mc.thePlayer!!.movementInput.moveForward > 0.0f || mc.thePlayer!!.movementInput.moveStrafe > 0.0f) {
            val speed = 4.25
            if (mc.thePlayer!!.onGround) {
                mc.thePlayer!!.jump()
                if (motionTicks == 1) {
                    tickTimer.reset()
                    if (move) {
                        mc.thePlayer!!.motionX = 0.0
                        mc.thePlayer!!.motionZ = 0.0
                        move = false
                    }
                    motionTicks = 0
                } else motionTicks = 1
            } else if (!move && motionTicks == 1 && tickTimer.hasTimePassed(5)) {
                mc.thePlayer!!.motionX *= speed
                mc.thePlayer!!.motionZ *= speed
                move = true
            }
            if (!mc.thePlayer!!.onGround) MovementUtils.strafe()
            tickTimer.update()
        }
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}