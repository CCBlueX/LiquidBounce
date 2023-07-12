package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.aacMotion2
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import org.lwjgl.input.Keyboard

object AAC3313 : FlyMode("AAC3.3.13") {
	private var wasDead = false

	override fun onUpdate() {
		if (mc.thePlayer.isDead)
			wasDead = true

		if (wasDead || mc.thePlayer.onGround) {
			wasDead = false
			mc.thePlayer.motionY = aacMotion2.toDouble()
			mc.thePlayer.onGround = false
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