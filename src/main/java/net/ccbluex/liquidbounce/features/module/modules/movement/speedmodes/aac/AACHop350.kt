/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object AACHop350 : SpeedMode("AACHop3.5.0") {

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val player = mc.player ?: return

        if (event.eventState == EventState.POST && isMoving && !player.isTouchingWater && !player.isTouchingLava && !mc.player.isSneaking) {
            player.jumpMovementFactor += 0.00208f
            if (player.fallDistance <= 1f) {
                if (player.onGround) {
                    player.tryJump()
                    player.velocityX *= 1.0118f
                    player.velocityZ *= 1.0118f
                } else {
                    player.velocityY -= 0.0147f
                    player.velocityX *= 1.00138f
                    player.velocityZ *= 1.00138f
                }
            }
        }
    }

    override fun onEnable() {
        val player = mc.player ?: return

        if (player.onGround) {
            player.velocityX = 0.0
            player.velocityZ = 0.0
        }
    }

    override fun onDisable() {
        mc.player?.jumpMovementFactor = 0.02f
    }
}