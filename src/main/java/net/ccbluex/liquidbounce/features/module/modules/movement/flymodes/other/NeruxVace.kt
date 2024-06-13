/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.neruxVaceTicks
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object NeruxVace : FlyMode("NeruxVace") {
	private var tick = 0
	override fun onUpdate() {
		if (!mc.thePlayer.onGround)
			tick++

		if (tick >= neruxVaceTicks && !mc.thePlayer.onGround) {
			tick = 0
			mc.thePlayer.motionY = .015
		}
	}
}
