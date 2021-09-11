package net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode

class AAC3_3_12Glide : FlyMode("AAC3.3.12-Glide")
{
	private var ticks = 0

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return
		val timer = mc.timer

		val onGround = thePlayer.onGround

		if (!onGround) ticks++

		if (ticks < 12)
		{
			when (ticks)
			{
				2 -> timer.timerSpeed = 1f
				12 -> timer.timerSpeed = 0.1f
			}
		}
		else if (!onGround)
		{
			ticks = 0
			thePlayer.motionY = .015
		}
	}
}
