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
        val player = mc.thePlayer

        if (player == null || !isMoving)
            return

        if (player.fallDistance > 3.994)
            return
        if (player.isInWater || player.isOnLadder || player.isCollidedHorizontally)
            return

        player.posY -= 0.3993000090122223
        player.motionY = -1000.0
        player.cameraPitch = 0.3f
        player.distanceWalkedModified = 44f
        mc.timer.timerSpeed = 1f

        if (player.onGround) {
            player.posY += 0.3993000090122223
            player.motionY = 0.3993000090122223
            player.distanceWalkedOnStepModified = 44f
            player.motionX *= 1.590000033378601
            player.motionZ *= 1.590000033378601
            player.cameraPitch = 0f
            mc.timer.timerSpeed = 1.199f
        }
    }

}