/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object VulcanHop : SpeedMode("VulcanHop") {
    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (player.isInWater || player.isInLava || player.isInWeb || player.isOnLadder) return

        if (isMoving) {
            if (player.isAirBorne && player.fallDistance > 2) {
                mc.timer.timerSpeed = 1f
                return
            }

            if (player.onGround) {
                player.tryJump()
                if (player.motionY > 0) {
                    mc.timer.timerSpeed = 1.1253f
                }
                strafe(0.4815f)
            } else {
                if (player.motionY < 0) {
                    mc.timer.timerSpeed = 0.8935f
                }
            }
        } else {
            mc.timer.timerSpeed = 1f
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }
}