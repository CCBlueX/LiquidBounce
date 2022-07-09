package net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode

class AAC3_3_13HighJump : FlyMode("AAC3.3.13-HighJump")
{
	private var wasDead = false

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return
		val timer = mc.timer

		val onGround = thePlayer.onGround

		if (thePlayer.isDead) wasDead = true

		if (wasDead || onGround)
		{
			wasDead = false
			thePlayer.motionY = Fly.aac3_3_13_MotionValue.get().toDouble()
			thePlayer.onGround = false
		}

		timer.timerSpeed = 1f

		if (mc.gameSettings.keyBindSneak.isKeyDown) // Help you to MLG
		{
			timer.timerSpeed = 0.2f
			mc.rightClickDelayTimer = 0
		}
	}

	override fun onDisable()
	{
		wasDead = false
	}
}
