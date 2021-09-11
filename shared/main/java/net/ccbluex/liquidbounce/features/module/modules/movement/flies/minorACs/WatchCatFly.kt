package net.ccbluex.liquidbounce.features.module.modules.movement.flies.minorACs

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class WatchCatFly : FlyMode("WatchCat")
{
	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return

		val y = thePlayer.posY

		MovementUtils.strafe(thePlayer, 0.15f)
		thePlayer.sprinting = true

		if (y < Fly.startY + 2)
		{
			thePlayer.motionY = Math.random() * 0.5
			return
		}

		if (Fly.startY > y) MovementUtils.strafe(thePlayer, 0f)
	}
}
