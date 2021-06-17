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

// Original author: RedeskySpeed v1 by noom#0681
// https://forums.ccbluex.net/topic/2110/very-fast-speed-for-redesky
class RedeSkySlowHop : SpeedMode("RedeSky-SlowHop")
{
	private var delay = 0
	private var boost = 0F

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return

		if (!MovementUtils.isMoving(thePlayer) || MovementUtils.cantBoostUp(thePlayer)) return

		if (thePlayer.onGround)
		{
			thePlayer.jump()
			delay = 0
			boost = 0F
		}
		else
		{
			delay++
			boost += 0.006F

			if (delay in 1..9)
			{
				thePlayer.jumpMovementFactor = 0.02F + boost
				thePlayer.motionY -= boost
			}
		}
	}

	override fun onMotion(eventState: EventState)
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}

	override fun onEnable()
	{
		delay = 0
		boost = 0F
	}

	override fun onDisable()
	{
		delay = 0
		boost = 0F
	}
}
