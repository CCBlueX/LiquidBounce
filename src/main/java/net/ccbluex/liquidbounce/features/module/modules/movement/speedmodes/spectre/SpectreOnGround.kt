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
        if (!isMoving || mc.player.movementInput.jump) return
        if (speedUp >= 10) {
            if (mc.player.onGround) {
                mc.player.velocityX = 0.0
                mc.player.velocityZ = 0.0
                speedUp = 0
            }
            return
        }
        if (mc.player.onGround && mc.options.forwardKey.isPressed) {
            val f = mc.player.rotationYaw.toRadians()
            mc.player.velocityX -= sin(f) * 0.145f
            mc.player.velocityZ += cos(f) * 0.145f
            event.x = mc.player.velocityX
            event.y = 0.005
            event.z = mc.player.velocityZ
            speedUp++
        }
    }
}