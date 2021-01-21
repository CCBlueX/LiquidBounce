/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class AAC3_1_7YPort : SpeedMode("AAC3.1.7-YPort")
{
	override fun onMotion()
	{
		if (MovementUtils.isMoving)
		{
			val thePlayer = mc.thePlayer ?: return

			thePlayer.cameraPitch = 0f
			if (thePlayer.onGround)
			{
				thePlayer.jump()
				thePlayer.motionY = 0.3851
				thePlayer.motionX *= 1.01
				thePlayer.motionZ *= 1.01
			} else thePlayer.motionY = -0.21
		}
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
