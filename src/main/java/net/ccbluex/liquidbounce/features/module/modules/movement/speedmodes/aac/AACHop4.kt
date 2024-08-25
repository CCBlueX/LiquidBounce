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
        val thePlayer = mc.player ?: return

        mc.ticker.timerSpeed = 1f

        if (!isMoving || thePlayer.isTouchingWater || thePlayer.isTouchingLava || thePlayer.isClimbing || thePlayer.isRiding)
            return

        if (thePlayer.onGround)
            thePlayer.tryJump()
        else {
            if (thePlayer.fallDistance <= 0.1)
                mc.ticker.timerSpeed = 1.5f
            else if (thePlayer.fallDistance < 1.3)
                mc.ticker.timerSpeed = 0.7f
            else
                mc.ticker.timerSpeed = 1f
        }
    }

}