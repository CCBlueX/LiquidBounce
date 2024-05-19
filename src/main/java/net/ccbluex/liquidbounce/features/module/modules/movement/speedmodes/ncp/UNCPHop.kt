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
        player?.stopXZ()
    }

    private fun airSpeedReset() {
        player.speedInAir = 0.02f
    }

    override fun onUpdate() {
        if (player.isInLava || player.isInWater
            || player.isOnLadder || player.isInWeb) {
            player.stopXZ()
            return
        }

        if (isMoving) {
            if (player.onGround) {
                player.tryJump()
                strafe(0.035f)
                player.speedInAir = 0.035f
            } else {
                if (!keyDown) {
                    strafe(0.228f)
                    player.speedInAir = 0.065f
                }
            }

            // Prevent from getting flag while airborne/falling & fall damage
            if (player.isAirBorne && player.fallDistance >= 3) {
                player.stopXZ()
                airSpeedReset()
            }

        } else {
            player.stopXZ()
        }
    }

    override fun onMove(event: MoveEvent) {
        if (mc.gameSettings.keyBindLeft.isKeyDown || mc.gameSettings.keyBindRight.isKeyDown) {
            keyDown = true
            strafe(0.2f)
            player.speedInAir = 0.055f
        } else {
            keyDown = false
        }
    }
}