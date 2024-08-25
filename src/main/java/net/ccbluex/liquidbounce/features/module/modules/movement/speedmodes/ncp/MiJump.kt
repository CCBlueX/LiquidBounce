/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe

object MiJump : SpeedMode("MiJump") {
    override fun onMotion() {
        if (!isMoving) return
        if (mc.player.onGround && !mc.player.movementInput.jump) {
            mc.player.motionY += 0.1
            val multiplier = 1.8
            mc.player.motionX *= multiplier
            mc.player.motionZ *= multiplier
            val currentSpeed = speed
            val maxSpeed = 0.66
            if (currentSpeed > maxSpeed) {
                mc.player.motionX = mc.player.motionX / currentSpeed * maxSpeed
                mc.player.motionZ = mc.player.motionZ / currentSpeed * maxSpeed
            }
        }
        strafe()
    }

}