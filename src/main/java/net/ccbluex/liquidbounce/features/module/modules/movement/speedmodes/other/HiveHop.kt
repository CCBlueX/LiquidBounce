/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe

object HiveHop : SpeedMode("HiveHop") {
    override fun onEnable() {
        mc.thePlayer.speedInAir = 0.0425f
        mc.timer.timerSpeed = 1.04f
    }

    override fun onDisable() {
        mc.thePlayer.speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
    }

    override fun onUpdate() {
        if (isMoving) {
            if (mc.thePlayer.onGround) mc.thePlayer.motionY = 0.3
            mc.thePlayer.speedInAir = 0.0425f
            mc.timer.timerSpeed = 1.04f
            strafe()
        } else {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            mc.thePlayer.speedInAir = 0.02f
            mc.timer.timerSpeed = 1f
        }
    }

}