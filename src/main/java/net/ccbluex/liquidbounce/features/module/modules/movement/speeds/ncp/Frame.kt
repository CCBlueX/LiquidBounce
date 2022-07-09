/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timer.TickTimer

class Frame : SpeedMode("Frame")
{
	private var motionTicks = 0
	private var move = false
	private val tickTimer = TickTimer()

	override fun onMotion(eventState: EventState)
	{
		if (eventState != EventState.PRE) return

		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.cantBoostUp) return

		if (thePlayer.isMoving)
		{
			val speed = 4.25

			if (thePlayer.onGround)
			{
				jump(thePlayer)

				if (motionTicks == 1)
				{
					tickTimer.reset()

					if (move)
					{
						thePlayer.zeroXZ()
						move = false
					}

					motionTicks = 0
				}
				else motionTicks = 1
			}
			else if (!move && motionTicks == 1 && tickTimer.hasTimePassed(5))
			{
				thePlayer.multiply(speed)

				move = true
			}

			if (!thePlayer.onGround) thePlayer.strafe()

			tickTimer.update()
		}
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
