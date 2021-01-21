/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class NCPFHop : SpeedMode("NCPFHop")
{
	override fun onEnable()
	{
		mc.timer.timerSpeed = 1.0866f
		super.onEnable()
	}

	override fun onDisable()
	{
		(mc.thePlayer ?: return).speedInAir = 0.02f
		mc.timer.timerSpeed = 1f
		super.onDisable()
	}

	override fun onMotion()
	{
	}

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return

		if (MovementUtils.isMoving)
		{
			if (thePlayer.onGround)
			{
				thePlayer.jump()
				thePlayer.motionX *= 1.01
				thePlayer.motionZ *= 1.01
				thePlayer.speedInAir = 0.0223f
			}
			thePlayer.motionY -= 0.00099999
			MovementUtils.strafe()
		} else
		{
			thePlayer.motionX = 0.0
			thePlayer.motionZ = 0.0
		}
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
