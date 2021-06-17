/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

// Original author: Co丶Dynamic
class AAC5BHop : SpeedMode("AAC5-BHop")
{
	var switch = false

	override fun onEnable()
	{
		switch = false
	}

	override fun onDisable()
	{
		mc.timer.timerSpeed = 1f
	}

	override fun onMotion(eventState: EventState)
	{
	}

	override fun onUpdate()
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (MovementUtils.cantBoostUp(thePlayer)) return

		val timer = mc.timer

		if (thePlayer.onGround) timer.timerSpeed = 1F

		if (thePlayer.onGround && MovementUtils.isMoving(thePlayer))
		{
			jump(thePlayer)
			timer.timerSpeed = 1F
			thePlayer.motionY = 0.419973
		}

		if (thePlayer.motionY > 0 && !thePlayer.onGround)
		{
			thePlayer.motionY -= 0.0007991
			thePlayer.jumpMovementFactor = 0.0201465F
			timer.timerSpeed = 1.82F
		}
		else
		{
			thePlayer.motionY -= 0.00074775
			thePlayer.jumpMovementFactor = 0.0201519F
			timer.timerSpeed = 0.92F
		}

		if (thePlayer.fallDistance > 2) timer.timerSpeed = 1F

		if (theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, 0.201, 0.0)).isNotEmpty() && theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox.offset(0.0, 0.199, 0.0)).isEmpty())
		{
			switch = !switch

			if (thePlayer.onGround && switch) timer.timerSpeed = 3F
		}
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
