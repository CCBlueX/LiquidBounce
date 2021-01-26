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

class AAC3_0_3BHop : SpeedMode("AAC3.0.3-BHop")
{
	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return
		if (eventState != EventState.PRE) return

		if (thePlayer.isInWater) return

		if (MovementUtils.isMoving)
		{
			mc.timer.timerSpeed = 1.08f

			if (thePlayer.onGround)
			{
				val dir = MovementUtils.direction
				thePlayer.motionX -= functions.sin(dir) * 0.2f
				thePlayer.motionZ += functions.cos(dir) * 0.2f

				thePlayer.motionY = 0.399
				LiquidBounce.eventManager.callEvent(JumpEvent(0.399f))

				mc.timer.timerSpeed = 2f
			} else
			{
				thePlayer.motionY *= 0.97
				thePlayer.motionX *= 1.008
				thePlayer.motionZ *= 1.008
			}
		} else
		{
			thePlayer.motionX = 0.0
			thePlayer.motionZ = 0.0
			mc.timer.timerSpeed = 1f
		}
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}

	override fun onDisable()
	{
		mc.timer.timerSpeed = 1f
	}
}
