/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spectre

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import kotlin.math.cos
import kotlin.math.sin

class SpectreOnGround : SpeedMode("Spectre-OnGround")
{
	private var speedUp = 0
	override fun onMotion()
	{
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		
		if (!MovementUtils.isMoving || thePlayer.movementInput.jump) return
		if (speedUp >= 10)
		{
			if (thePlayer.onGround)
			{
				thePlayer.motionX = 0.0
				thePlayer.motionZ = 0.0
				speedUp = 0
			}
			return
		}
		if (thePlayer.onGround && mc.gameSettings.keyBindForward.isKeyDown)
		{
			val f = thePlayer.rotationYaw * 0.017453292f
			thePlayer.motionX -= sin(f) * 0.145f
			thePlayer.motionZ += cos(f) * 0.145f
			event.x = thePlayer.motionX
			event.y = 0.005
			event.z = thePlayer.motionZ
			speedUp++
		}
	}
}
