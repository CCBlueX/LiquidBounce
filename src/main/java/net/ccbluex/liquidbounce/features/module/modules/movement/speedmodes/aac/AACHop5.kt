/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object AACHop5 : SpeedMode("AACHop5") {
    override fun onUpdate() {
        val thePlayer = mc.player ?: return

        if (!isMoving || thePlayer.isTouchingWater || thePlayer.isTouchingLava || thePlayer.isClimbing || thePlayer.isRiding)
            return

        if (thePlayer.onGround) {
            thePlayer.tryJump()
            mc.ticker.timerSpeed = 0.9385f
            thePlayer.speedInAir = 0.0201f
        }

        if (thePlayer.fallDistance < 2.5) {
            if (thePlayer.fallDistance > 0.7) {
                if (thePlayer.ticksAlive % 3 == 0) {
                    mc.ticker.timerSpeed = 1.925f
                } else if (mc.player.fallDistance < 1.25) {
                    mc.ticker.timerSpeed = 1.7975f
                }
            }
            thePlayer.speedInAir = 0.02f
        }
    }

}