package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object HawkEye : FlyMode("HawkEye") {
	override fun onUpdate() {
		mc.thePlayer.motionY = if (mc.thePlayer.motionY <= -0.42) 0.42 else -0.42
	}
}