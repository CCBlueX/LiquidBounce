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

object MatrixSlowHop : SpeedMode("MatrixSlowHop") {
    
    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (player.isInWater || player.isInLava || player.isInWeb || player.isOnLadder) return

        if (isMoving) {
            if (player.isAirBorne && player.fallDistance > 2) {
                mc.timer.timerSpeed = 1f
                player.speedInAir = 0.02f
                return
            }

            if (player.onGround) {
                player.tryJump()
                mc.timer.timerSpeed = 0.5195f
                strafe()
            } else {
                mc.timer.timerSpeed = 1.0973f
            }

            if (player.fallDistance <= 0.4 && player.moveStrafing == 0f) {
                player.speedInAir = 0.02035f
            } else {
                player.speedInAir = 0.02f
            }
        } else {
            mc.timer.timerSpeed = 1f
        }
    }
}
