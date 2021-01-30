/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class HypixelBHop : SpeedMode("HypixelHop")
{
	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return
		if (eventState != EventState.PRE) return

		if (MovementUtils.isMoving)
		{
			if (thePlayer.onGround)
			{
				jump(thePlayer)
				var speed = if (MovementUtils.speed < 0.56f) MovementUtils.speed * 1.045f else 0.56f
				if (thePlayer.onGround && thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED))) speed *= 1f + 0.13f * (1 + thePlayer.getActivePotionEffect(classProvider.getPotionEnum(PotionType.MOVE_SPEED))!!.amplifier)
				MovementUtils.strafe(speed)
				return
			}
			else if (thePlayer.motionY < 0.2) thePlayer.motionY -= 0.02
			MovementUtils.strafe(MovementUtils.speed * 1.01889f)
		}
		else
		{
			thePlayer.motionZ = 0.0
			thePlayer.motionX = thePlayer.motionZ
		}
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
