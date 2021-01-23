/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class NCPYPort : SpeedMode("NCPYPort")
{
	private var jumps = 0
	override fun onMotion()
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.isOnLadder || thePlayer.isInWater || thePlayer.isInLava || thePlayer.isInWeb || !MovementUtils.isMoving || thePlayer.isInWater) return
		if (jumps >= 4 && thePlayer.onGround) jumps = 0
		if (thePlayer.onGround)
		{
			thePlayer.motionY = if (jumps <= 1) 0.42 else 0.4
			val f = WMathHelper.toRadians(thePlayer.rotationYaw)
			thePlayer.motionX -= functions.sin(f) * 0.2f
			thePlayer.motionZ += functions.cos(f) * 0.2f
			jumps++
		} else if (jumps <= 1) thePlayer.motionY = -5.0
		MovementUtils.strafe()
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
