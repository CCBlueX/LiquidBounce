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

class SpectreBHop : SpeedMode("Spectre-BHop")
{
	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return
		if (eventState != EventState.PRE) return

		if (!MovementUtils.isMoving(thePlayer) || thePlayer.movementInput.jump) return

		if (thePlayer.onGround)
		{
			MovementUtils.strafe(thePlayer, 1.1f)
			thePlayer.motionY = 0.44
			return
		}

		MovementUtils.strafe(thePlayer)
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
