/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.aacFast
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object AAC305 : FlyMode("AAC3.0.5") {
	private var tick = 0
	
	override fun onUpdate() {
		if (tick == 2)
			mc.player.velocityY = 0.1
		else if (tick > 2) tick = 0

		if (aacFast) {
			if (mc.player.input.movementSideways == 0f) mc.player.flyingSpeed = 0.08f
			else mc.player.flyingSpeed = 0f
		}

		tick++
	}
}
