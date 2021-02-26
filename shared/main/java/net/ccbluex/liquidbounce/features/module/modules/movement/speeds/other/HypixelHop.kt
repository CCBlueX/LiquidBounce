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

class HypixelHop : SpeedMode("HypixelHop")
{
	override fun onMotion(eventState: EventState)
	{
		if (eventState != EventState.PRE) return

		val thePlayer = mc.thePlayer ?: return

		if (MovementUtils.isMoving(thePlayer))
		{
			if (thePlayer.onGround)
			{
				thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 9.1314E-4, thePlayer.posZ)// #344

				jump(thePlayer)

				MovementUtils.strafe(thePlayer, (if (MovementUtils.getSpeed(thePlayer) < 0.56f) MovementUtils.getSpeed(thePlayer) * 1.045f else 0.56f) * (1.0F + 0.13f * MovementUtils.getSpeedEffectAmplifier(thePlayer)))

				return
			}
			else if (thePlayer.motionY < 0.2) thePlayer.motionY -= 0.02

			MovementUtils.strafe(thePlayer, MovementUtils.getSpeed(thePlayer) * 1.01889f)
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
