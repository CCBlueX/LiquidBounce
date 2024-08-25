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

object OldMatrixHop : SpeedMode("OldMatrixHop") {
    
    override fun onUpdate() {
        val player = mc.player ?: return
        if (player.isTouchingWater || player.isTouchingLava || player.isInWeb() || player.isClimbing) return
        
        if (isMoving) {
            if (player.onGround) {
                player.tryJump()
                player.speedInAir = 0.02098f
                mc.ticker.timerSpeed = 1.055f
            } else {
                strafe()
            }    
        } else {
            mc.ticker.timerSpeed = 1f    
        }
    }
}
