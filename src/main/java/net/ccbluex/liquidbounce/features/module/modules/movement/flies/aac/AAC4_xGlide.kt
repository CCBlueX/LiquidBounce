package net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode

class AAC4_xGlide : FlyMode("AAC4.X-Glide")
{
	private var ticks = 0

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return
		val timer = mc.timer

		if (!thePlayer.onGround && !(thePlayer.isCollidedHorizontally || thePlayer.isCollidedVertically))
		{
			timer.timerSpeed = 0.6f

			if (thePlayer.motionY < 0 && ticks > 0)
			{
				ticks--
				timer.timerSpeed = 0.95f
			}
			else
			{
				ticks = 0
				thePlayer.motionY = thePlayer.motionY / 0.9800000190734863
				thePlayer.motionY += 0.03
				thePlayer.motionY *= 0.9800000190734863
				thePlayer.jumpMovementFactor = 0.03625f
			}
		}
		else
		{
			timer.timerSpeed = 1.0f
			ticks = 2
		}
	}
}
