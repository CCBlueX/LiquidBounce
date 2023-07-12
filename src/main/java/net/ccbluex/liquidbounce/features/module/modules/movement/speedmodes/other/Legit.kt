package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving

object Legit : SpeedMode("Legit") {
    override fun onMotion() {
        // NONE
    }

    override fun onUpdate() {
        if (mc.thePlayer == null) {
            return
        }
        if (mc.thePlayer.onGround && isMoving) {
            mc.thePlayer.jump()
        }
    }

    override fun onMove(event: MoveEvent) {
        // NONE
    }
}