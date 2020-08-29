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
        val thePlayer = mc.thePlayer

        if (thePlayer == null || !MovementUtils.isMoving)
            return

        if (thePlayer.fallDistance > 3.994)
            return
        if (thePlayer.isInWater || thePlayer.isOnLadder || thePlayer.isCollidedHorizontally)
            return

        thePlayer.posY -= 0.3993000090122223
        thePlayer.motionY = -1000.0
        thePlayer.cameraPitch = 0.3f
        thePlayer.distanceWalkedModified = 44.0f
        mc.timer.timerSpeed = 1f

        if (thePlayer.onGround) {
            thePlayer.posY += 0.3993000090122223
            thePlayer.motionY = 0.3993000090122223
            thePlayer.distanceWalkedOnStepModified = 44.0f
            thePlayer.motionX *= 1.590000033378601
            thePlayer.motionZ *= 1.590000033378601
            thePlayer.cameraPitch = 0.0f
            mc.timer.timerSpeed = 1.199f
        }
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}