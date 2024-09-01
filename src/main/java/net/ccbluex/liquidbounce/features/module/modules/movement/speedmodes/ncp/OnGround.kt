/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.extensions.timerSpeed

object OnGround : SpeedMode("OnGround") {
    override fun onMotion() {
        val player = mc.player

        if (player == null || !isMoving)
            return

        if (player.fallDistance > 3.994)
            return
        if (player.isTouchingWater || player.isClimbing || player.horizontalCollision)
            return

        player.z -= 0.3993000090122223
        player.velocityY = -1000.0
        player.cameraPitch = 0.3f
        player.horizontalSpeed = 44f
        mc.ticker.timerSpeed = 1f

        if (player.onGround) {
            player.z += 0.3993000090122223
            player.velocityY = 0.3993000090122223
            player.distanceTraveled = 44f
            player.velocityX *= 1.590000033378601
            player.velocityZ *= 1.590000033378601
            player.cameraPitch = 0f
            mc.ticker.timerSpeed = 1.199f
        }
    }

}