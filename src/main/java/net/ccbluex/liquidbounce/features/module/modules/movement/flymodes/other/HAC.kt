/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object HAC : FlyMode("HAC") {
	override fun onUpdate() {
		mc.player.velocityX *= 0.8
		mc.player.velocityZ *= 0.8
		mc.player.velocityY = if (mc.player.velocityY <= -0.42) 0.42 else -0.42
	}
}
