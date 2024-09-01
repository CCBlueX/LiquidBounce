/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.isInWeb
import net.ccbluex.liquidbounce.utils.extensions.timerSpeed
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object VulcanLowHop : SpeedMode("VulcanLowHop") {
    override fun onUpdate() {
        val player = mc.player ?: return
        if (player.isTouchingWater || player.isTouchingLava || player.isInWeb() || player.isClimbing) return

        if (isMoving) {
            if (!player.onGround && player.fallDistance > 1.1) {
                mc.ticker.timerSpeed = 1F
                player.velocityY = -0.25
                return
            }

            if (player.onGround) {
                player.tryJump()
                strafe(0.4815f)
                mc.ticker.timerSpeed = 1.263f
            } else if (player.ticksAlive % 4 == 0) {
                if (player.ticksAlive % 3 == 0) {
                    player.velocityY = -0.01 / player.velocityY
                } else {
                    player.velocityY = -player.velocityY / player.y
                }
                mc.ticker.timerSpeed = 0.8985f
            }

        } else {
            mc.ticker.timerSpeed = 1f
        }
    }
}