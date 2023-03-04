package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving

class Legit: SpeedMode("Legit") {
    override fun onMotion() {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }
}