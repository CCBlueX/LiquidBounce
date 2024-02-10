/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object SlowHop : SpeedMode("SlowHop") {
    override fun onMotion() {
        val player = mc.thePlayer ?: return
        if (player.isInWater || player.isInLava || player.isInWeb || player.isOnLadder) return
        
        if (isMoving) {
            if (player.onGround) player.tryJump() else speed *= 1.011f
        } else {
            player.motionX = 0.0
            player.motionZ = 0.0
        }
    }

}