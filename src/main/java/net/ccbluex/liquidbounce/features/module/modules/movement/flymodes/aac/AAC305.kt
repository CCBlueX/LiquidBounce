package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.aacFast
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object AAC305 : FlyMode("AAC3.0.5") {
	private var tick = 0
	
	override fun onUpdate() {
		if (tick == 2)
			mc.thePlayer.motionY = 0.1
		else if (tick > 2) tick = 0

		if (aacFast) {
			if (mc.thePlayer.movementInput.moveStrafe == 0f) mc.thePlayer.jumpMovementFactor = 0.08f
			else mc.thePlayer.jumpMovementFactor = 0f
		}

		tick++
	}
}