package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.minecraft.potion.Potion

object VerusLowHop : SpeedMode("VerusLowHop") {

    var speed = 0.0f
    var airTicks = 0

    override fun onMotion() {
        if (MovementUtils.isMoving) {
            if (mc.thePlayer.onGround) {
                airTicks = 0
                if(mc.thePlayer.isPotionActive(Potion.moveSpeed) && mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier == 1) {
                    speed = 0.5f
                } else {
                    speed = 0.36f
                }

                mc.thePlayer.jump()
            } else {
                if(airTicks == 0) {
                    mc.thePlayer.motionY = -0.09800000190734863
                }

                airTicks++
                speed *= 0.98f;
            }

            strafe(speed, false)
        }
    }
}