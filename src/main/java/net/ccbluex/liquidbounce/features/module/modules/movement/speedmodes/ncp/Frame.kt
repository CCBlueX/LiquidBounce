/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.timing.TickTimer

object Frame : SpeedMode("Frame") {
    private var motionTicks = 0
    private var move = false
    private val tickTimer = TickTimer()
    override fun onMotion() {
        if (isMoving) {
            val speed = 4.25
            if (mc.player.onGround) {
                mc.player.tryJump()
                if (motionTicks == 1) {
                    tickTimer.reset()
                    if (move) {
                        mc.player.motionX = 0.0
                        mc.player.motionZ = 0.0
                        move = false
                    }
                    motionTicks = 0
                } else motionTicks = 1
            } else if (!move && motionTicks == 1 && tickTimer.hasTimePassed(5)) {
                mc.player.motionX *= speed
                mc.player.motionZ *= speed
                move = true
            }
            if (!mc.player.onGround) strafe()
            tickTimer.update()
        }
    }

}