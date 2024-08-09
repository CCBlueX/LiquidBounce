/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object MineBlazeTimer : SpeedMode("MineBlazeTimer") {
    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        mc.timer.timerSpeed = 1f

        if (!isMoving || player.isInWater || player.isInLava || player.isOnLadder || player.isRiding)
            return

        if (player.onGround)
            player.tryJump()
        else {
            if (player.fallDistance <= 0.1)
                mc.timer.timerSpeed = 1.7f
            else if (player.fallDistance < 1.3)
                mc.timer.timerSpeed = 0.8f
            else
                mc.timer.timerSpeed = 1f
        }
    }

}
