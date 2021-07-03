package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.minorACs

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
class DaedalusAACBHop : SpeedMode("DaedalusAAC-BHop")
{
	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return

		if (MovementUtils.cantBoostUp(thePlayer)) return

		if (isMoving(thePlayer))
		{
			strafe(thePlayer, 0.3f)

			if (thePlayer.onGround) jump(thePlayer)

			strafe(thePlayer)
		}
		else MovementUtils.zeroXZ(thePlayer)
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
