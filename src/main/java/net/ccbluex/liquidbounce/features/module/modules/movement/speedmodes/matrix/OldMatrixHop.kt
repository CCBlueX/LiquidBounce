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
        if (player.isInWater || player.isInLava || player.isInWeb || player.isOnLadder) return
        
        if (isMoving) {
            if (player.onGround) {
                player.tryJump()
                player.speedInAir = 0.02098f
                mc.timer.timerSpeed = 1.055f
            } else {
                strafe()
            }    
        } else {
            mc.timer.timerSpeed = 1f    
        }
    }
}
