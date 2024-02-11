/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object AAC3BHop : SpeedMode("AAC3BHop") {
    private var legitJump = false

    override fun onTick() {
        val thePlayer = mc.thePlayer ?: return

        mc.timer.timerSpeed = 1f

        if (thePlayer.isInWater || thePlayer.isInLava)
            return

        if (isMoving) {
            when {
                thePlayer.onGround -> {
                    if (legitJump) {
                        thePlayer.tryJump()
                        legitJump = false
                        return
                    }
                    thePlayer.motionY = 0.3852
                    thePlayer.onGround = false
                    strafe(0.374f)
                }
                thePlayer.motionY < 0.0 -> {
                    thePlayer.speedInAir = 0.0201f
                    mc.timer.timerSpeed = 1.02f
                }
                else -> mc.timer.timerSpeed = 1.01f
            }
        } else {
            legitJump = true
            thePlayer.motionX = 0.0
            thePlayer.motionZ = 0.0
        }
    }

    override fun onDisable() {
        mc.thePlayer.speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
    }
}
