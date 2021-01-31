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

class AAC3_3_11FastBHop : SpeedMode("AAC3.3.11-FastBHop")
{
	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return

		if (!MovementUtils.isMoving || thePlayer.ridingEntity != null || thePlayer.hurtTime > 0) return

		if (thePlayer.onGround)
		{
			jump(thePlayer)
			thePlayer.motionY = 0.405
			thePlayer.motionX *= 1.004
			thePlayer.motionZ *= 1.004
			return
		}

		MovementUtils.strafe()
	}

	override fun onMotion(eventState: EventState)
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
