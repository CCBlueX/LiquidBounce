package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.minorACs

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.*

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
class ACRBHop : SpeedMode("ACR-BHop")
{
	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.cantBoostUp) return

		if (thePlayer.isMoving)
		{
			thePlayer.jumpMovementFactor = 0.1F

			thePlayer.divide(1.1)

			if (thePlayer.onGround) jump(thePlayer)

			thePlayer.strafe()
		}
		else thePlayer.zeroXZ()
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
