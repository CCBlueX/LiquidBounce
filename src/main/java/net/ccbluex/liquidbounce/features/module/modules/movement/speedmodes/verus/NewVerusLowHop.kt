package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.minecraft.potion.Potion

object NewVerusLowHop : SpeedMode("NewVerusLowHop") {

    private var speed = 0.0f
    private var airTicks = 0

    override fun onUpdate() {
        if (isMoving) {
            if (mc.thePlayer.onGround) {
                airTicks = 0

                // Checks for Speed potion effect 1 & 2
                if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                    val amplifier = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier

                    speed = when (amplifier) {
                        1 -> 0.55f
                        2 -> 0.7f
                        else -> 0.33f
                    }
                }

                // Checks for Slowness potion effect
                speed = if (mc.thePlayer.isPotionActive(Potion.moveSlowdown)
                    && mc.thePlayer.getActivePotionEffect(Potion.moveSlowdown).amplifier == 1) {
                    0.3f
                } else {
                    0.33f
                }

                mc.thePlayer.jump()
            } else {
                if (airTicks == 0) {
                    mc.thePlayer.motionY = -0.09800000190734863
                }

                airTicks++
                speed *= 0.99f
            }

            strafe(speed, false)
        }
    }
}
