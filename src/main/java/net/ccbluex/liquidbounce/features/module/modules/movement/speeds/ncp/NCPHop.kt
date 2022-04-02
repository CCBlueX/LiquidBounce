/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class NCPHop : SpeedMode("NCPHop") {
    override fun onEnable() {
        mc.timer.timerSpeed = 1.0865f
        super.onEnable()
    }

    override fun onDisable() {
        mc.thePlayer!!.speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
        super.onDisable()
    }

    override fun onMotion() {}
    override fun onUpdate() {
        if (MovementUtils.isMoving) {
            if (mc.thePlayer!!.onGround) {
                mc.thePlayer!!.jump()
                mc.thePlayer!!.speedInAir = 0.0223f
            }
            MovementUtils.strafe()
        } else {
            mc.thePlayer!!.motionX = 0.0
            mc.thePlayer!!.motionZ = 0.0
        }
    }

    override fun onMove(event: MoveEvent) {}
}