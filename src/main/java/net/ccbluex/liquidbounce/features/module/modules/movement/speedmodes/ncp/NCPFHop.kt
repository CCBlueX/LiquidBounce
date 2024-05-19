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
        player.speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
        super.onDisable()
    }

    override fun onUpdate() {
        if (isMoving) {
            if (player.onGround) {
                player.tryJump()
                player.motionX *= 1.01
                player.motionZ *= 1.01
                player.speedInAir = 0.0223f
            }
            player.motionY -= 0.00099999
            strafe()
        } else {
            player.motionX = 0.0
            player.motionZ = 0.0
        }
    }

}