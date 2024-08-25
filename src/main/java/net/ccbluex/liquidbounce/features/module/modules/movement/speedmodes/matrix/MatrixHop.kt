/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object MatrixHop : SpeedMode("MatrixHop") {

    override fun onUpdate()  {
        val player = mc.player ?: return
        if (player.isTouchingWater || player.isTouchingLava || player.isInWeb() || player.isClimbing) return

        if (isMoving) {
            if (player.isAirBorne && player.fallDistance > 1.215f) {
                mc.ticker.timerSpeed = 1f
                return
            }

            if (player.onGround) {
                strafe()
                player.tryJump()
                if (player.velocityY > 0) {
                    mc.ticker.timerSpeed = 1.0953f
                }
            } else {
                if (player.velocityY < 0) {
                    mc.ticker.timerSpeed = 0.9185f
                }
            }
        } else {
            mc.ticker.timerSpeed = 1f
        }
    }
}
