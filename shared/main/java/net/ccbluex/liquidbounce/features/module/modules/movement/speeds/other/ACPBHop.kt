package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
class ACPBHop : SpeedMode("ACP-BHop")
{
	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return

		if (MovementUtils.isMoving(thePlayer))
		{
			val moveSpeed = when (MovementUtils.getSpeedEffectAmplifier(thePlayer))
			{
				1 -> 0.85F // 0.31 +6 +6 +
				2 -> 0.91F // 0.37 - previous value
				3 -> 1.01F // 0.41
				4 -> 1.12F // 0.45
				5 -> 1.23F // 0.49
				6 -> 1.35F // 0.53
				else -> 0.55F
			}

			MovementUtils.strafe(thePlayer, moveSpeed)

			if (thePlayer.onGround) jump(thePlayer)

			MovementUtils.strafe(thePlayer)
		}
		else
		{
			thePlayer.motionX = 0.0
			thePlayer.motionZ = 0.0
		}
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
