/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.*

class AAC3_3_13LowHop : SpeedMode("AAC3.3.13-LowHop") // Was AACHop3.3.13
{
	override fun onMotion(eventState: EventState)
	{
	}

	override fun onUpdate()
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (!thePlayer.isMoving || thePlayer.cantBoostUp || thePlayer.hurtTime > 0) return
		if (thePlayer.onGround && thePlayer.isCollidedVertically)
		{
			// boost = 0.202, y = 0.405

			thePlayer.boost(0.202f)

			thePlayer.motionY = 0.405
			LiquidBounce.eventManager.callEvent(JumpEvent(0.405f))

			thePlayer.strafe()
		}
		else if (thePlayer.fallDistance < 0.31f)
		{
			if (classProvider.isBlockCarpet(theWorld.getBlock(thePlayer.position))) // why?
				return

			// Motion XZ
			thePlayer.jumpMovementFactor = if (thePlayer.moveStrafing == 0f) 0.027f else 0.021f

			thePlayer.multiply(1.001)

			// Motion Y
			if (!thePlayer.isCollidedHorizontally) thePlayer.motionY -= 0.014999993f
		}
		else thePlayer.jumpMovementFactor = 0.02f
	}

	override fun onMove(event: MoveEvent)
	{
	}

	override fun onDisable()
	{
		(mc.thePlayer ?: return).jumpMovementFactor = 0.02f
	}
}
