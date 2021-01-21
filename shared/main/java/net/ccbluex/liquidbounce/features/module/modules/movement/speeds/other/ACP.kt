package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import kotlin.math.cos
import kotlin.math.sin

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
class ACP : SpeedMode("AntiCheatPlus")
{
	override fun onMotion()
	{
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (isMoving)
		{
			val yaw = direction
			val amplifier = if (thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED))) thePlayer.getActivePotionEffect(classProvider.getPotionEnum(PotionType.MOVE_SPEED))!!.amplifier else -1
			thePlayer.motionX *= 0.8
			thePlayer.motionZ *= 0.8
			var moveSpeed = 0.55F
			when (amplifier)
			{
				0 -> moveSpeed = 0.85F // 0.31 +6 +6 +
				1 -> moveSpeed = 0.91F // 0.37 - previous value
				2 -> moveSpeed = 1.01F // 0.41
				3 -> moveSpeed = 1.12F // 0.45
				4 -> moveSpeed = 1.23F // 0.49
				5 -> moveSpeed = 1.35F // 0.53
				else ->
				{
				}
			}

			event.x = -sin(yaw) * moveSpeed
			event.z = cos(yaw) * moveSpeed
		}
	}
}
