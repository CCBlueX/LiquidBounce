/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving

object OnGround : SpeedMode("OnGround") {
    override fun onMotion() {
        val thePlayer = mc.player

        if (thePlayer == null || !isMoving)
            return

        if (thePlayer.fallDistance > 3.994)
            return
        if (thePlayer.isTouchingWater || thePlayer.isClimbing || thePlayer.isCollidedHorizontally)
            return

        theplayer.z -= 0.3993000090122223
        thePlayer.velocityY = -1000.0
        thePlayer.cameraPitch = 0.3f
        thePlayer.distanceWalkedModified = 44f
        mc.ticker.timerSpeed = 1f

        if (thePlayer.onGround) {
            theplayer.z += 0.3993000090122223
            thePlayer.velocityY = 0.3993000090122223
            thePlayer.distanceWalkedOnStepModified = 44f
            thePlayer.velocityX *= 1.590000033378601
            thePlayer.velocityZ *= 1.590000033378601
            thePlayer.cameraPitch = 0f
            mc.ticker.timerSpeed = 1.199f
        }
    }

}