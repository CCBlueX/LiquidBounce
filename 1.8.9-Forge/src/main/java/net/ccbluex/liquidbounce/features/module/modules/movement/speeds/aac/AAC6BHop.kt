/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class AAC6BHop : SpeedMode("AAC6BHop") {
    private var legitJump = false
    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        mc.timer.timerSpeed = 1f

        if (thePlayer.isInWater)
            return
        if (MovementUtils.isMoving) {
            if (thePlayer.onGround) {
                if (legitJump) {
                    thePlayer.motionY = 0.4
                    MovementUtils.strafe(0.15f)
                    thePlayer.onGround = false
                    legitJump = false

                    return
                }
                thePlayer.motionY = 0.41
                MovementUtils.strafe(0.47458485f)
            }

            if (thePlayer.motionY < 0 && thePlayer.motionY > -0.2)
                mc.timer.timerSpeed = (1.2 + thePlayer.motionY).toFloat()

            thePlayer.speedInAir = 0.022151f
        } else {
            legitJump = true
            thePlayer.motionX = 0.0
            thePlayer.motionZ = 0.0
        }
    }

    override fun onMotion() {}

    override fun onMove(event: MoveEvent) {}
    override fun onEnable() {
        legitJump = true
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        mc.thePlayer?.speedInAir = 0.02f
    }
}