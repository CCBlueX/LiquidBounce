/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object HawkEye : FlyMode("HawkEye") {
	override fun onUpdate() {
		mc.player.velocityY = if (mc.player.velocityY <= -0.42) 0.42 else -0.42
	}
}
