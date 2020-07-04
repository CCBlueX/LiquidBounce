/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spartan

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode

class SpartanYPort : SpeedMode("SpartanYPort") {
    private var airMoves = 0
    override fun onMotion() {
        if (mc.gameSettings.keyBindForward.isKeyDown && !mc.gameSettings.keyBindJump.isKeyDown) {
            if (mc.thePlayer!!.onGround) {
                mc.thePlayer!!.jump()
                airMoves = 0
            } else {
                mc.timer.timerSpeed = 1.08f
                if (airMoves >= 3) mc.thePlayer!!.jumpMovementFactor = 0.0275f
                if (airMoves >= 4 && airMoves % 2.toDouble() == 0.0) {
                    mc.thePlayer!!.motionY = -0.32f - 0.009 * Math.random()
                    mc.thePlayer!!.jumpMovementFactor = 0.0238f
                }
                airMoves++
            }
        }
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}