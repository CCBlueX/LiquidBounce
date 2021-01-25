package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.vanilla

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
class VanillaHop : SpeedMode("VanillaHop")
{
	override fun onMotion(eventState: EventState)
	{
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (MovementUtils.isMoving)
		{
			val moveSpeed = (LiquidBounce.moduleManager[Speed::class.java] as Speed).vanillaSpeedValue.get()
			val dir = MovementUtils.direction
			event.x = (-functions.sin(dir) * moveSpeed).toDouble()
			event.z = (functions.cos(dir) * moveSpeed).toDouble()

			if (thePlayer.onGround) jump(thePlayer)
		}
	}
}
