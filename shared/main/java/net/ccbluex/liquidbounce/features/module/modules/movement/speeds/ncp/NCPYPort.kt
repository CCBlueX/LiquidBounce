/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class NCPYPort : SpeedMode("NCPYPort")
{
	private var jumps = 0

	override fun onMotion(eventState: EventState)
	{
		if (eventState != EventState.PRE) return

		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.isOnLadder || thePlayer.isInWater || thePlayer.isInLava || thePlayer.isInWeb || !MovementUtils.isMoving(thePlayer) || thePlayer.isInWater) return
		if (jumps >= 4 && thePlayer.onGround) jumps = 0
		if (thePlayer.onGround)
		{
			val motion = if (jumps <= 1) 0.42f else 0.4f
			val dir = MovementUtils.getDirection(thePlayer)

			thePlayer.motionY = motion.toDouble()
			thePlayer.motionX -= functions.sin(dir) * 0.2f
			thePlayer.motionZ += functions.cos(dir) * 0.2f
			LiquidBounce.eventManager.callEvent(JumpEvent(motion))

			jumps++
		}
		else if (jumps <= 1) thePlayer.motionY = -5.0
		MovementUtils.strafe(thePlayer)
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
