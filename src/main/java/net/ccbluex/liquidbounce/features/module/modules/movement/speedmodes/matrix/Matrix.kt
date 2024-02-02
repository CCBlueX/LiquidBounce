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

object Matrix : SpeedMode("Matrix") {
    
    override fun onUpdate() {
        if (mc.thePlayer.isInWater || mc.thePlayer.isInLava || mc.thePlayer.isInWeb || mc.thePlayer.isOnLadder) return
        if (isMoving) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.tryJump()
                mc.thePlayer.speedInAir = 0.02098f
                mc.timer.timerSpeed = 1.055f
            } else {
                strafe()
            }    
        } else {
            mc.timer.timerSpeed = 1f    
        }
    }
    
    override fun onDisable() {
        mc.thePlayer.speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
    }
}
