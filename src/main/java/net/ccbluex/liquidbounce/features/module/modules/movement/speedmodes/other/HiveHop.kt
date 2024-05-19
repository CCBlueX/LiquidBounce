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
        player.speedInAir = 0.0425f
        mc.timer.timerSpeed = 1.04f
    }

    override fun onDisable() {
        player.speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
    }

    override fun onUpdate() {
        if (isMoving) {
            if (player.onGround) player.motionY = 0.3
            player.speedInAir = 0.0425f
            mc.timer.timerSpeed = 1.04f
            strafe()
        } else {
            player.motionX = 0.0
            player.motionZ = 0.0
            player.speedInAir = 0.02f
            mc.timer.timerSpeed = 1f
        }
    }

}