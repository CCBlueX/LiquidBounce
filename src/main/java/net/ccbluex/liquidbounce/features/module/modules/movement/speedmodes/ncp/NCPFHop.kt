/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object NCPFHop : SpeedMode("NCPFHop") {
    override fun onEnable() {
        mc.timer.timerSpeed = 1.0866f
        super.onEnable()
    }

    override fun onDisable() {
        mc.player.speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
        super.onDisable()
    }

    override fun onUpdate() {
        if (isMoving) {
            if (mc.player.onGround) {
                mc.player.tryJump()
                mc.player.velocityX *= 1.01
                mc.player.velocityZ *= 1.01
                mc.player.speedInAir = 0.0223f
            }
            mc.player.velocityY -= 0.00099999
            strafe()
        } else {
            mc.player.velocityX = 0.0
            mc.player.velocityZ = 0.0
        }
    }

}