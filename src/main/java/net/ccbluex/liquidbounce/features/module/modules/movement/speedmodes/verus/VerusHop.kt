package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.minecraft.potion.Potion

object VerusHop : SpeedMode("VerusHop") {

    private var speed = 0.0f

    override fun onMotion() {
        if (MovementUtils.isMoving) {
            if (mc.thePlayer.onGround) {
                if (mc.thePlayer.isPotionActive(Potion.moveSpeed) && mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier == 1) {
                    speed = 0.46f
                } else {
                    speed = 0.34f
                }

                mc.thePlayer.jump()
            } else {
                speed *= 0.98f
            }
            strafe(speed, false)
        }
    }
}
