/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spartan

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.cantBoostUp
import net.ccbluex.liquidbounce.utils.extensions.isMoving

class SpartanYPort : SpeedMode("Spartan-YPort")
{
	private var airTicks = 0

	override fun onMotion(eventState: EventState)
	{
		if (eventState != EventState.PRE) return

		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.cantBoostUp) return

		if (thePlayer.isMoving && !thePlayer.movementInput.jump)
		{
			if (thePlayer.onGround)
			{
				jump(thePlayer)

				airTicks = 0
			}
			else
			{
				mc.timer.timerSpeed = 1.08f

				val airticks = airTicks

				if (airticks >= 3) thePlayer.jumpMovementFactor = 0.0275f

				if (airticks >= 4 && airticks % 2.0 == 0.0)
				{
					thePlayer.motionY = -0.32f - 0.009 * Math.random()
					thePlayer.jumpMovementFactor = 0.0238f
				}

				airTicks++
			}
		}
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
