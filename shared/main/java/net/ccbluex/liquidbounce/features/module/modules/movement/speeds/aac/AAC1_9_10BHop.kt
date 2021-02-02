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

class AAC1_9_10BHop : SpeedMode("AAC1.9.10-BHop") // Was OldAACBHop
{
	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return
		if (eventState != EventState.PRE) return

		if (MovementUtils.isMoving)
		{
			if (thePlayer.onGround)
			{
				MovementUtils.strafe(0.56f)

				thePlayer.motionY = 0.41999998688697815
				LiquidBounce.eventManager.callEvent(JumpEvent(0.42F))
			}
			else MovementUtils.strafe(MovementUtils.speed * if (thePlayer.fallDistance > 0.4f) 1.0f else 1.01f)
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
