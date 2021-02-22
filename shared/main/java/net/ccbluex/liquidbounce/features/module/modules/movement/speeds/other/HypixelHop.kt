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

class HypixelHop : SpeedMode("HypixelHop")
{
	override fun onMotion(eventState: EventState)
	{
		if (eventState != EventState.PRE) return

		val thePlayer = mc.thePlayer ?: return

		if (MovementUtils.isMoving(thePlayer))
		{
			if (thePlayer.onGround)
			{
				jump(thePlayer)
				var speed = if (MovementUtils.getSpeed(thePlayer) < 0.56f) MovementUtils.getSpeed(thePlayer) * 1.045f else 0.56f
				if (thePlayer.onGround && thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED))) speed *= 1f + 0.13f * MovementUtils.getSpeedEffectAmplifier(thePlayer)
				MovementUtils.strafe(thePlayer, speed)
				return
			}
			else if (thePlayer.motionY < 0.2) thePlayer.motionY -= 0.02
			MovementUtils.strafe(thePlayer, MovementUtils.getSpeed(thePlayer) * 1.01889f)
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
