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
		if (player.isDead)
			wasDead = true

		if (wasDead || player.onGround) {
			wasDead = false
			player.motionY = aacMotion2.toDouble()
			player.onGround = false
		}

		mc.timer.timerSpeed = 1f

		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
			mc.timer.timerSpeed = 0.2f
			mc.rightClickDelayTimer = 0
		}
	}

	override fun onDisable() {
		wasDead = false
	}
}
