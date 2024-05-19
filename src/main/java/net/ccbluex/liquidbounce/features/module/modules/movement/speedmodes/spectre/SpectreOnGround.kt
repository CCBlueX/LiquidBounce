/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import kotlin.math.cos
import kotlin.math.sin

object SpectreOnGround : SpeedMode("SpectreOnGround") {
    private var speedUp = 0
    override fun onMove(event: MoveEvent) {
        if (!isMoving || player.movementInput.jump) return
        if (speedUp >= 10) {
            if (player.onGround) {
                player.motionX = 0.0
                player.motionZ = 0.0
                speedUp = 0
            }
            return
        }
        if (player.onGround && mc.gameSettings.keyBindForward.isKeyDown) {
            val f = player.rotationYaw.toRadians()
            player.motionX -= sin(f) * 0.145f
            player.motionZ += cos(f) * 0.145f
            event.x = player.motionX
            event.y = 0.005
            event.z = player.motionZ
            speedUp++
        }
    }
}