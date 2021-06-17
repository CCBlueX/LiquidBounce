/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class MatrixBHop : SpeedMode("Matrix-BHop")
{

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.isInWater) return

		val timer = mc.timer

		if (MovementUtils.cantBoostUp(thePlayer)) return

		if (MovementUtils.isMoving(thePlayer))
		{
			if (thePlayer.onGround)
			{
				jump(thePlayer)

				thePlayer.speedInAir = 0.02098f
				timer.timerSpeed = 1.055f
			}
			else MovementUtils.strafe(thePlayer, MovementUtils.getSpeed(thePlayer))
		}
		else timer.timerSpeed = 1f
	}

	override fun onMotion(eventState: EventState)
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}

	override fun onDisable()
	{
		(mc.thePlayer ?: return).speedInAir = 0.02f
		mc.timer.timerSpeed = 1f
	}
}
