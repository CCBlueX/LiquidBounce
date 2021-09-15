/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
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

		if (MovementUtils.cantBoostUp(thePlayer)) return

		if (MovementUtils.isMoving(thePlayer))
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
						MovementUtils.zeroXZ(thePlayer)
						move = false
					}

					motionTicks = 0
				}
				else motionTicks = 1
			}
			else if (!move && motionTicks == 1 && tickTimer.hasTimePassed(5))
			{
				MovementUtils.multiply(thePlayer, speed)

				move = true
			}

			if (!thePlayer.onGround) MovementUtils.strafe(thePlayer)

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
