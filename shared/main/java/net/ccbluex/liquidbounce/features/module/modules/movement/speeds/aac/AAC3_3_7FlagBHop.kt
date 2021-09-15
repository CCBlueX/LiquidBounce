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

class AAC3_3_7FlagBHop : SpeedMode("AAC3.3.7-FlagBHop") // Was AAC4BHop
{
	private var shouldLegitJump = true

	override fun onTick()
	{
		val thePlayer = mc.thePlayer ?: return

		if (MovementUtils.cantBoostUp(thePlayer)) return

		if (MovementUtils.isMoving(thePlayer))
		{
			if (shouldLegitJump)
			{
				if (thePlayer.onGround)
				{
					jump(thePlayer)

					thePlayer.onGround = false

					shouldLegitJump = false
				}
				return
			}

			if (thePlayer.onGround)
			{
				thePlayer.onGround = false

				MovementUtils.strafe(thePlayer, 0.375f)

				jump(thePlayer)

				thePlayer.motionY = 0.41
			}
			else thePlayer.speedInAir = 0.0211f
		}
		else
		{
			shouldLegitJump = true

			MovementUtils.zeroXZ(thePlayer)
		}
	}

	override fun onMotion(eventState: EventState)
	{
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}

	override fun onEnable()
	{
		shouldLegitJump = true
	}

	override fun onDisable()
	{
		(mc.thePlayer ?: return).speedInAir = 0.02f
	}
}
