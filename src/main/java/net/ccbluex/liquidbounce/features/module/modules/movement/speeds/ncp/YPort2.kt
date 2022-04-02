/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class YPort2 : SpeedMode("YPort2") {
    override fun onMotion() {
        if (mc.thePlayer!!.isOnLadder || mc.thePlayer!!.isInWater || mc.thePlayer!!.isInLava || mc.thePlayer!!.isInWeb || !MovementUtils.isMoving)
            return
        if (mc.thePlayer!!.onGround) mc.thePlayer!!.jump() else mc.thePlayer!!.motionY = -1.0
        MovementUtils.strafe()
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}