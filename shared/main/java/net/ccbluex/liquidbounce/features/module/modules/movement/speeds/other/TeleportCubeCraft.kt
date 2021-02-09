/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer

class TeleportCubeCraft : SpeedMode("TeleportCubeCraft")
{
	private val timer = MSTimer()

	override fun onMotion(eventState: EventState)
	{
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (MovementUtils.isMoving(thePlayer) && thePlayer.onGround && timer.hasTimePassed(300L))
		{
			val dir = MovementUtils.getDirection(thePlayer)
			val length = ((LiquidBounce.moduleManager[Speed::class.java] as Speed?) ?: return).cubecraftPortLengthValue.get()
			event.x = (-functions.sin(dir) * length).toDouble()
			event.z = (functions.cos(dir) * length).toDouble()
			timer.reset()
		}
	}
}
