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

object UNCPHop : SpeedMode("UNCPHop") {

    private var keyDown = false

    override fun onDisable() {
        airSpeedReset()
        mc.thePlayer?.stopXZ()
    }

    private fun airSpeedReset() {
        mc.thePlayer.speedInAir = 0.02f
    }

    override fun onUpdate() {
        if (mc.thePlayer.isInLava || mc.thePlayer.isInWater
            || mc.thePlayer.isOnLadder || mc.thePlayer.isInWeb) {
            mc.thePlayer.stopXZ()
            return
        }

        if (isMoving) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.tryJump()
                strafe(0.035f)
                mc.thePlayer.speedInAir = 0.035f
            } else {
                if (!keyDown) {
                    strafe(0.228f)
                    mc.thePlayer.speedInAir = 0.065f
                }
            }

            // Prevent from getting flag while airborne/falling & fall damage
            if (mc.thePlayer.isAirBorne && mc.thePlayer.fallDistance >= 3) {
                mc.thePlayer.stopXZ()
                airSpeedReset()
            }

        } else {
            mc.thePlayer.stopXZ()
        }
    }

    override fun onMove(event: MoveEvent) {
        if (mc.gameSettings.keyBindLeft.isKeyDown || mc.gameSettings.keyBindRight.isKeyDown) {
            keyDown = true
            strafe(0.2f)
            mc.thePlayer.speedInAir = 0.055f
        } else {
            keyDown = false
        }
    }
}