/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving

object AACYPort : SpeedMode("AACYPort") {
    override fun onMotion() {
        val thePlayer = mc.thePlayer ?: return

        if (isMoving && !thePlayer.isSneaking) {
            thePlayer.cameraPitch = 0f

            if (thePlayer.onGround) {
                thePlayer.motionY = 0.3425
                thePlayer.motionX *= 1.5893
                thePlayer.motionZ *= 1.5893
            } else
                thePlayer.motionY = -0.19
        }
    }

}