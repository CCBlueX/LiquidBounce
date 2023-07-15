/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.minecraft.potion.Potion

object HypixelHop : SpeedMode("HypixelHop") {
    override fun onMotion() {
        if (isMoving) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump()
                var targetSpeed = if (speed < 0.56f) speed * 1.045f else 0.56f
                if (mc.thePlayer.onGround && mc.thePlayer.isPotionActive(Potion.moveSpeed)) targetSpeed *= 1f + 0.13f * (1 + mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier)
                strafe(targetSpeed)
                return
            } else if (mc.thePlayer.motionY < 0.2) mc.thePlayer.motionY -= 0.02

            speed *= 1.01889f
        } else {
            mc.thePlayer.motionZ = 0.0
            mc.thePlayer.motionX = mc.thePlayer.motionZ
        }
    }

}