package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
class PACBHop : SpeedMode("PhoenixAC-BHop")
{
	override fun onMotion(eventState: EventState)
	{
		val thePlayer = mc.thePlayer ?: return

		if (MovementUtils.isMoving)
		{
			val moveSpeed = when (if (thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED))) thePlayer.getActivePotionEffect(classProvider.getPotionEnum(PotionType.MOVE_SPEED))!!.amplifier else -1)
			{
				0 -> 0.40F // 0.31 +6 +6 +
				1 -> 0.48F // 0.37 - previous value
				2 -> 0.56F // 0.41
				3 -> 0.63F // 0.45
				4 -> 0.71F // 0.49
				5 -> 0.80F // 0.53
				else -> 0.33F
			}

			MovementUtils.strafe(moveSpeed)

			if (thePlayer.onGround) jump(thePlayer)

			MovementUtils.strafe()
		} else
		{
			thePlayer.motionX = 0.0
			thePlayer.motionZ = 0.0
		}
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
