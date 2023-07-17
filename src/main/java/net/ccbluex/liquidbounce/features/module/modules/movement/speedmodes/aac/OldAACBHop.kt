/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe

object OldAACBHop : SpeedMode("OldAACBHop") {
    override fun onMotion() {
        val thePlayer = mc.thePlayer ?: return

        if (isMoving) {
            if (thePlayer.onGround) {
                strafe(0.56f)
                thePlayer.motionY = 0.41999998688697815
            } else speed *= if (thePlayer.fallDistance > 0.4f) 1f else 1.01f
        } else {
            thePlayer.motionX = 0.0
            thePlayer.motionZ = 0.0
        }
    }

}