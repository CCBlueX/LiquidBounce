/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object UNCPHop : SpeedMode("UNCPHop") {

    private var keyDown = false

    override fun onDisable() {
        airSpeedReset()
        mc.player?.stopXZ()
    }

    private fun airSpeedReset() {
        mc.player.speedInAir = 0.02f
    }

    override fun onUpdate() {
        if (mc.player.isTouchingLava || mc.player.isTouchingWater
            || mc.player.isClimbing || mc.player.isInWeb()) {
            mc.player.stopXZ()
            return
        }

        if (isMoving) {
            if (mc.player.onGround) {
                mc.player.tryJump()
                strafe(0.035f)
                mc.player.speedInAir = 0.035f
            } else {
                if (!keyDown) {
                    strafe(0.228f)
                    mc.player.speedInAir = 0.065f
                }
            }

            // Prevent from getting flag while airborne/falling & fall damage
            if (mc.player.velocityDirty && mc.player.fallDistance >= 3) {
                mc.player.stopXZ()
                airSpeedReset()
            }

        } else {
            mc.player.stopXZ()
        }
    }

    override fun onMove(event: MoveEvent) {
        if (mc.options.leftKey.isPressed || mc.options.rightKey.isPressed) {
            keyDown = true
            strafe(0.2f)
            mc.player.speedInAir = 0.055f
        } else {
            keyDown = false
        }
    }
}