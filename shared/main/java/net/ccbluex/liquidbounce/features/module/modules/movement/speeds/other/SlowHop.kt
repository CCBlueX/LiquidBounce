/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class SlowHop : SpeedMode("SlowHop") {
    override fun onMotion() {
        if (mc.thePlayer!!.isInWater) return
        if (MovementUtils.isMoving) {
            if (mc.thePlayer!!.onGround) mc.thePlayer!!.jump() else MovementUtils.strafe(MovementUtils.speed * 1.011f)
        } else {
            mc.thePlayer!!.motionX = 0.0
            mc.thePlayer!!.motionZ = 0.0
        }
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}