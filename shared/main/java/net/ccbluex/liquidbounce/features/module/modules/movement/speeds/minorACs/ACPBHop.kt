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
class ACPBHop : SpeedMode("ACP-BHop")
{
	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.cantBoostUp) return

		if (thePlayer.isMoving)
		{
			val moveSpeed = when (thePlayer.speedEffectAmplifier)
			{
				1 -> 0.85F // 0.31 +6 +6 +
				2 -> 0.91F // 0.37 - previous value
				3 -> 1.01F // 0.41
				4 -> 1.12F // 0.45
				5 -> 1.23F // 0.49
				6 -> 1.35F // 0.53
				else -> 0.55F
			}

			thePlayer.strafe(moveSpeed)

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
