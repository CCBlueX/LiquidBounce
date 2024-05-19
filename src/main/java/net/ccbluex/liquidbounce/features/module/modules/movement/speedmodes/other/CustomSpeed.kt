/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe

object CustomSpeed : SpeedMode("Custom") {
    override fun onMotion() {
        if (isMoving) {
            mc.timer.timerSpeed = Speed.customTimer
            when {
                player.onGround -> {
                    strafe(Speed.customSpeed)
                    player.motionY = Speed.customY.toDouble()
                }
                Speed.customStrafe -> strafe(Speed.customSpeed)
                else -> strafe()
            }
        } else {
            player.motionZ = 0.0
            player.motionX = player.motionZ
        }
    }

    override fun onEnable() {
        if (Speed.resetXZ) {
            player.motionZ = 0.0
            player.motionX = player.motionZ
        }
        if (Speed.resetY) player.motionY = 0.0
        super.onEnable()
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        super.onDisable()
    }

}