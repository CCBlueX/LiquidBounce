/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class AAC3_0_5BHop : SpeedMode("AAC3.0.5-BHop") // Was AAC2BHop
{
	override fun onMotion(eventState: EventState)
	{
		if (eventState != EventState.PRE) return

		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.isInWater) return

		if (MovementUtils.isMoving(thePlayer))
		{
			if (thePlayer.onGround)
			{
				jump(thePlayer)

				thePlayer.motionX *= 1.02
				thePlayer.motionZ *= 1.02
			}
			else if (thePlayer.motionY > -0.2)
			{
				thePlayer.motionY += 0.0143099999999999999999999999999
				thePlayer.jumpMovementFactor = 0.07f
			}
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
