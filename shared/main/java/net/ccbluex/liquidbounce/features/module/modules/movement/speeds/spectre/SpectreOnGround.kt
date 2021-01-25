/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spectre

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class SpectreOnGround : SpeedMode("Spectre-OnGround")
{
	private var speedUp = 0

	override fun onMotion(eventState: EventState)
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
			val yawRadians = WMathHelper.toRadians(thePlayer.rotationYaw)
			thePlayer.motionX -= functions.sin(yawRadians) * 0.145f
			thePlayer.motionZ += functions.cos(yawRadians) * 0.145f
			event.x = thePlayer.motionX
			event.y = 0.005
			event.z = thePlayer.motionZ
			speedUp++
		}
	}
}
