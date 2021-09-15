/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class AAC3_3_11BHop : SpeedMode("AAC3.3.11-BHop") // Was AAC7BHop
{
	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return

		if (!MovementUtils.isMoving(thePlayer) || MovementUtils.cantBoostUp(thePlayer) || thePlayer.hurtTime > 0) return

		if (thePlayer.onGround)
		{
			jump(thePlayer)

			MovementUtils.multiply(thePlayer, 1.004)

			thePlayer.motionY = 0.405
			LiquidBounce.eventManager.callEvent(JumpEvent(0.405f))

			return
		}

		MovementUtils.strafe(thePlayer)
	}

	override fun onMotion(eventState: EventState)
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
