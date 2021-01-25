/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spartan

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode

class SpartanYPort : SpeedMode("Spartan-YPort")
{
	private var airMoves = 0

	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return
		if (eventState != EventState.PRE) return

		if (mc.gameSettings.keyBindForward.isKeyDown && !mc.gameSettings.keyBindJump.isKeyDown)
		{
			if (thePlayer.onGround)
			{
				thePlayer.jump()
				airMoves = 0
			} else
			{
				mc.timer.timerSpeed = 1.08f
				if (airMoves >= 3) thePlayer.jumpMovementFactor = 0.0275f
				if (airMoves >= 4 && airMoves % 2.0 == 0.0)
				{
					thePlayer.motionY = -0.32f - 0.009 * Math.random()
					thePlayer.jumpMovementFactor = 0.0238f
				}
				airMoves++
			}
		}
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
