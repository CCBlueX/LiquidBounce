/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextDouble

object SpartanYPort : SpeedMode("SpartanYPort") {
    private var airMoves = 0
    override fun onMotion() {
        if (mc.options.forwardKey.isPressed) {
            if (mc.player.onGround) {
                mc.player.tryJump()
                airMoves = 0
            } else {
                mc.ticker.timerSpeed = 1.08f
                if (airMoves >= 3) mc.player.flyingSpeed = 0.0275f
                if (airMoves >= 4 && airMoves % 2 == 0) {
                    mc.player.velocityY = -0.32 - nextDouble(endInclusive = 0.009)
                    mc.player.flyingSpeed = 0.0238f
                }
                airMoves++
            }
        }
    }

}