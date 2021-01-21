/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class MiJump : SpeedMode("MiJump")
{
	override fun onMotion()
	{
		val thePlayer = mc.thePlayer ?: return

		if (!MovementUtils.isMoving) return
		if (thePlayer.onGround && !thePlayer.movementInput.jump)
		{
			thePlayer.motionY += 0.1
			val multiplier = 1.8
			thePlayer.motionX *= multiplier
			thePlayer.motionZ *= multiplier
			val currentSpeed = Math.sqrt(Math.pow(thePlayer.motionX, 2.0) + Math.pow(thePlayer.motionZ, 2.0))
			val maxSpeed = 0.66
			if (currentSpeed > maxSpeed)
			{
				thePlayer.motionX = thePlayer.motionX / currentSpeed * maxSpeed
				thePlayer.motionZ = thePlayer.motionZ / currentSpeed * maxSpeed
			}
		}
		MovementUtils.strafe()
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
