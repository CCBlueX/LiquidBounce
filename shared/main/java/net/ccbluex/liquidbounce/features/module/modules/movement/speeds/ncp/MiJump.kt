/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class MiJump : SpeedMode("MiJump")
{
	override fun onMotion(eventState: EventState)
	{
		if (eventState != EventState.PRE) return

		val thePlayer = mc.thePlayer ?: return

		if (!MovementUtils.isMoving(thePlayer) || MovementUtils.cantBoostUp(thePlayer)) return

		if (thePlayer.onGround && !thePlayer.movementInput.jump)
		{
			thePlayer.motionY += 0.1

			val multiplier = 1.8

			MovementUtils.multiply(thePlayer, multiplier)

			val speed = MovementUtils.getSpeed(thePlayer)
			val maxSpeed = 0.66

			if (speed > maxSpeed) MovementUtils.divide(thePlayer, speed * maxSpeed)
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
