/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe

object HypixelHop : SpeedMode("HypixelHop") {
    override fun onStrafe() {
        if (mc.thePlayer.isInWater || mc.thePlayer.isInLava)
            return

        if (mc.thePlayer.onGround && isMoving) {
            if (mc.thePlayer.isUsingItem) {
                mc.thePlayer.jump()
            } else {
                mc.thePlayer.jump()
                strafe(0.4f)
            }
        }

    }
}
