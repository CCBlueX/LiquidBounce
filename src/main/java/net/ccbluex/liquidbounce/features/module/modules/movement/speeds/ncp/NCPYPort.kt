/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import kotlin.math.cos
import kotlin.math.sin

class NCPYPort : SpeedMode("NCPYPort") {
    private var jumps = 0
    override fun onMotion() {
        if (mc.thePlayer!!.isOnLadder || mc.thePlayer!!.isInWater || mc.thePlayer!!.isInLava || mc.thePlayer!!.isInWeb || !MovementUtils.isMoving || mc.thePlayer!!.isInWater) return
        if (jumps >= 4 && mc.thePlayer!!.onGround) jumps = 0
        if (mc.thePlayer!!.onGround) {
            mc.thePlayer!!.motionY = if (jumps <= 1) 0.42 else 0.4
            val f = mc.thePlayer!!.rotationYaw * 0.017453292f
            mc.thePlayer!!.motionX -= sin(f) * 0.2f
            mc.thePlayer!!.motionZ += cos(f) * 0.2f
            jumps++
        } else if (jumps <= 1) mc.thePlayer!!.motionY = -5.0
        MovementUtils.strafe()
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}