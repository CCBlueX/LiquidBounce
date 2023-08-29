package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving


object MineBlazeTimer : SpeedMode("MineBlazeTimer") {
    override fun onUpdate() {
        if (mc.thePlayer == null) {
            return
          }
          if (mc.thePlayer.onGround && isMoving) {
              mc.thePlayer.jump()
            }
            if (!mc.thePlayer.onGround && mc.thePlayer.fallDistance <= 0.1) {
                mc.timer.timerSpeed = 1.4f
          }
            if (mc.thePlayer.fallDistance > 0.1 && mc.thePlayer.fallDistance < 1.3) {
                mc.timer.timerSpeed = 0.7f
          }
            if (mc.thePlayer.fallDistance >= 1.3) {
                mc.timer.timerSpeed = 1f
            }
        }
    }
