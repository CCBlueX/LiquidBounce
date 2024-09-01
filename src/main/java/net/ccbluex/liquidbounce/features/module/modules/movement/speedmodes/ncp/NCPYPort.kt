/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import kotlin.math.cos
import kotlin.math.sin

object NCPYPort : SpeedMode("NCPYPort") {
    private var jumps = 0
    override fun onMotion() {
        if (mc.player.isClimbing || mc.player.isTouchingWater || mc.player.isTouchingLava || mc.player.isInWeb() || !isMoving || mc.player.isTouchingWater) return
        if (jumps >= 4 && mc.player.onGround) jumps = 0
        if (mc.player.onGround) {
            mc.player.velocityY = if (jumps <= 1) 0.42 else 0.4
            val f = mc.player.yaw.toRadians()
            mc.player.velocityX -= sin(f) * 0.2f
            mc.player.velocityZ += cos(f) * 0.2f
            jumps++
        } else if (jumps <= 1) mc.player.velocityY = -5.0
        strafe()
    }

}