/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object AACHop4 : SpeedMode("AACHop4") {
    override fun onUpdate() {
        val player = mc.player ?: return

        mc.ticker.timerSpeed = 1f

        if (!isMoving || player.isTouchingWater || player.isTouchingLava || player.isClimbing || player.isRiding)
            return

        if (player.onGround)
            player.tryJump()
        else {
            if (player.fallDistance <= 0.1)
                mc.ticker.timerSpeed = 1.5f
            else if (player.fallDistance < 1.3)
                mc.ticker.timerSpeed = 0.7f
            else
                mc.ticker.timerSpeed = 1f
        }
    }

}