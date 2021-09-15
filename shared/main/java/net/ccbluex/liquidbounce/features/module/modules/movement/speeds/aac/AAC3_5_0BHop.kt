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

class AAC3_5_0BHop : SpeedMode("AAC3.5.0-BHop") // Was AACHop3.5.0
{
	override fun onMotion(eventState: EventState)
	{
		if (eventState != EventState.POST) return

		val thePlayer = mc.thePlayer ?: return

		if (!MovementUtils.isMoving(thePlayer) || MovementUtils.cantBoostUp(thePlayer)) return

		thePlayer.jumpMovementFactor += 0.00208f
		if (thePlayer.fallDistance <= 1f)
		{
			if (thePlayer.onGround)
			{
				jump(thePlayer)

				MovementUtils.multiply(thePlayer, 1.0118f)
			}
			else
			{
				MovementUtils.multiply(thePlayer, 1.00138f)

				thePlayer.motionY -= 0.0147f
			}
		}
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}

	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.onGround)
		{
			thePlayer.motionZ = 0.0
			thePlayer.motionX = thePlayer.motionZ
		}
	}

	override fun onDisable()
	{
		(mc.thePlayer ?: return).jumpMovementFactor = 0.02f
	}
}
