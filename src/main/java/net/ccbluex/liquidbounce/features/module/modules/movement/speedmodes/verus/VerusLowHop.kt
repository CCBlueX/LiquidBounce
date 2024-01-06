package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.minecraft.potion.Potion

object VerusLowHop : SpeedMode("VerusLowHop") {

    private var speed = 0.0f
    private var airTicks = 0

    override fun onUpdate() {
        if (isMoving) {
            if (mc.thePlayer.onGround) {
                airTicks = 0
                speed = if (mc.thePlayer.isPotionActive(Potion.moveSpeed)
                    && mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier >= 1)
                        0.5f else 0.36f

                mc.thePlayer.tryJump()
            } else {
                if (airTicks == 0) {
                    mc.thePlayer.motionY = -0.09800000190734863
                }

                airTicks++
                speed *= 0.98f
            }

            strafe(speed, false)
        }
    }
}
