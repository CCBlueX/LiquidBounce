package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextDouble

object WatchCat : FlyMode("WatchCat") {
	override fun onUpdate() {
		strafe(0.15f)
		mc.thePlayer.isSprinting = true

		if (mc.thePlayer.posY < startY + 2) {
			mc.thePlayer.motionY = nextDouble(endInclusive = 0.5)
			return
		}

		if (startY > mc.thePlayer.posY) mc.thePlayer.stopXZ()
	}
}