/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextDouble

object WatchCat : FlyMode("WatchCat") {
	override fun onUpdate() {
		strafe(0.15f)
		player.isSprinting = true

		if (player.posY < startY + 2) {
			player.motionY = nextDouble(endInclusive = 0.5)
			return
		}

		if (startY > player.posY) player.stopXZ()
	}
}