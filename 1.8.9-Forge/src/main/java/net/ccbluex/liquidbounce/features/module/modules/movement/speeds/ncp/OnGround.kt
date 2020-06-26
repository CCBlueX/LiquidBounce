/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class OnGround : SpeedMode("OnGround") {
    override fun onMotion() {
        if (!MovementUtils.isMoving()) return
        if (mc.thePlayer!!.fallDistance > 3.994) return
        if (mc.thePlayer!!.isInWater() || mc.thePlayer!!.isOnLadder() || mc.thePlayer!!.isCollidedHorizontally) return
        mc.thePlayer!!.posY -= 0.3993000090122223
        mc.thePlayer!!.motionY = -1000.0
        mc.thePlayer!!.cameraPitch = 0.3f
        mc.thePlayer.distanceWalkedModified = 44.0f
        mc.timer.timerSpeed = 1f
        if (mc.thePlayer!!.onGround) {
            mc.thePlayer!!.posY += 0.3993000090122223
            mc.thePlayer!!.motionY = 0.3993000090122223
            mc.thePlayer.distanceWalkedOnStepModified = 44.0f
            mc.thePlayer!!.motionX *= 1.590000033378601
            mc.thePlayer!!.motionZ *= 1.590000033378601
            mc.thePlayer!!.cameraPitch = 0.0f
            mc.timer.timerSpeed = 1.199f
        }
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}