/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object HAC : FlyMode("HAC") {
	override fun onUpdate() {
		mc.thePlayer.motionX *= 0.8
		mc.thePlayer.motionZ *= 0.8
		mc.thePlayer.motionY = if (mc.thePlayer.motionY <= -0.42) 0.42 else -0.42
	}
}
