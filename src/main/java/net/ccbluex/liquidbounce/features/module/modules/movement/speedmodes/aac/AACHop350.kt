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
        val thePlayer = mc.player ?: return

        if (event.eventState == EventState.POST && isMoving && !thePlayer.isTouchingWater && !thePlayer.isTouchingLava && !mc.player.isSneaking) {
            thePlayer.jumpMovementFactor += 0.00208f
            if (thePlayer.fallDistance <= 1f) {
                if (thePlayer.onGround) {
                    thePlayer.tryJump()
                    thePlayer.velocityX *= 1.0118f
                    thePlayer.velocityZ *= 1.0118f
                } else {
                    thePlayer.velocityY -= 0.0147f
                    thePlayer.velocityX *= 1.00138f
                    thePlayer.velocityZ *= 1.00138f
                }
            }
        }
    }

    override fun onEnable() {
        val thePlayer = mc.player ?: return

        if (thePlayer.onGround) {
            thePlayer.velocityX = 0.0
            thePlayer.velocityZ = 0.0
        }
    }

    override fun onDisable() {
        mc.player?.jumpMovementFactor = 0.02f
    }
}