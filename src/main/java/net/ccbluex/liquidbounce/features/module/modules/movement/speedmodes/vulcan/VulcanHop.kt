/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object VulcanHop : SpeedMode("VulcanHop") {
    override fun onUpdate() {
        val player = mc.player ?: return
        if (player.isTouchingWater || player.isTouchingLava || player.isInWeb() || player.isClimbing) return

        if (isMoving) {
            if (player.velocityDirty && player.fallDistance > 2) {
                mc.ticker.timerSpeed = 1f
                return
            }

            if (player.onGround) {
                player.tryJump()
                if (player.velocityY > 0) {
                    mc.ticker.timerSpeed = 1.1453f
                }
                strafe(0.4815f)
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