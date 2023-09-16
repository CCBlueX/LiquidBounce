package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object Vulcan : FlyMode("Vulcan") {
    override fun onUpdate() {
        if (!mc.thePlayer.onGround && mc.thePlayer.fallDistance > 0) {
            if (mc.thePlayer.ticksExisted % 2 == 0) {
                mc.thePlayer.motionY = -0.155
            } else {
                mc.thePlayer.motionY = -0.1
            }
        }
    }
}
