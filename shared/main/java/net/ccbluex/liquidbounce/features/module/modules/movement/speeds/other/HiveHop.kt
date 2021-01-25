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

class HiveHop : SpeedMode("HiveHop")
{
	override fun onEnable()
	{
		mc.thePlayer!!.speedInAir = 0.0425f
		mc.timer.timerSpeed = 1.04f
	}

	override fun onDisable()
	{
		mc.thePlayer!!.speedInAir = 0.02f
		mc.timer.timerSpeed = 1f
	}

	override fun onMotion(eventState: EventState)
	{
	}

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return
		
		if (MovementUtils.isMoving)
		{
			if (thePlayer.onGround) thePlayer.motionY = 0.3
			thePlayer.speedInAir = 0.0425f
			mc.timer.timerSpeed = 1.04f
			MovementUtils.strafe()
		} else
		{
			thePlayer.motionZ = 0.0
			thePlayer.motionX = thePlayer.motionZ
			thePlayer.speedInAir = 0.02f
			mc.timer.timerSpeed = 1f
		}
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
