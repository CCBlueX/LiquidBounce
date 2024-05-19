/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object MineBlazeHop : SpeedMode("MineBlazeHop") {
    override fun onUpdate() {
        player ?: return

        if (player.isInWater || player.isInLava || player.isInWeb || player.isOnLadder) return
        
        if (player.onGround && isMoving) {
            player.tryJump()
        }

        if (player.motionY > 0.003) {
            player.motionX *= 1.0015
            player.motionZ *= 1.0015
            mc.timer.timerSpeed = 1.06f
        }
    }
}
