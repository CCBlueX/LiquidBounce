/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class AACLowHop2 : SpeedMode("AACLowHop2") {
    private var legitJump = false

    override fun onEnable() {
        legitJump = true
        mc.timer.timerSpeed = 1f
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    override fun onMotion() {
        val thePlayer = mc.thePlayer ?: return

        mc.timer.timerSpeed = 1f

        if (thePlayer.isInWater)
            return

        if (MovementUtils.isMoving) {
            mc.timer.timerSpeed = 1.09f

            if (thePlayer.onGround) {
                if (legitJump) {
                    thePlayer.jump()
                    legitJump = false

                    return
                }

                thePlayer.motionY = 0.343
                MovementUtils.strafe(0.534f)
            }
        } else {
            legitJump = true
            thePlayer.motionX = 0.0
            thePlayer.motionZ = 0.0
        }
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}