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
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class Mineplex : SpeedMode("Mineplex")
{

	private var speed1 = 0f
	private var speed2 = 0f
	private var wfg = false
	private var fallDistance = 0f

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return

		val x = thePlayer.posX - thePlayer.prevPosX
		val z = thePlayer.posZ - thePlayer.prevPosZ
		val distance = hypot(x, z).toFloat()
		if (MovementUtils.isMoving && thePlayer.onGround)
		{
			thePlayer.motionY = 0.4052393
			wfg = true
			speed2 = speed1
			speed1 = 0f
		} else
		{
			if (wfg)
			{
				speed1 = (speed2 + (0.46532f * min(fallDistance, 1f)))
				wfg = false
			} else speed1 = distance * 0.936f
			fallDistance = thePlayer.fallDistance
		}
		var minimum = 0f
		if (!wfg) minimum = 0.399900111f
		val strafe = max(min(speed1, 2f), minimum)
		MovementUtils.strafe(strafe)
	}

	override fun onMotion(eventState: EventState)
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}

	override fun onDisable()
	{
		speed1 = 0f
		speed2 = 0f
		wfg = false
		fallDistance = 0f
	}
}
