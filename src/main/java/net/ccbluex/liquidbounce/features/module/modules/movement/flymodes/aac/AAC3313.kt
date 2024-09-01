/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.aacMotion2
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import org.lwjgl.input.Keyboard

object AAC3313 : FlyMode("AAC3.3.13") {
	private var wasDead = false

	override fun onUpdate() {
		if (mc.!player.isAlive)
			wasDead = true

		if (wasDead || mc.player.onGround) {
			wasDead = false
			mc.player.velocityY = aacMotion2.toDouble()
			mc.player.onGround = false
		}

		mc.ticker.timerSpeed = 1f

		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
			mc.ticker.timerSpeed = 0.2f
			mc.rightClickDelayTimer = 0
		}
	}

	override fun onDisable() {
		wasDead = false
	}
}
