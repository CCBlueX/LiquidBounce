/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spectre

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class SpectreLowHop : SpeedMode("Spectre-LowHop")
{
	override fun onMotion(eventState: EventState)
	{
		if (eventState != EventState.PRE) return

		val thePlayer = mc.thePlayer ?: return
		if (thePlayer.movementInput.jump) return

		if (MovementUtils.cantBoostUp(thePlayer)) return

		if (MovementUtils.isMoving(thePlayer))
		{
			if (thePlayer.onGround)
			{
				MovementUtils.strafe(thePlayer, 1.1f)
				thePlayer.motionY = 0.15
			}

			MovementUtils.strafe(thePlayer)
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
