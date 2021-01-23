package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
class ACPBHop : SpeedMode("AntiCheatPlus-BHop")
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
			val dir = direction
			val amplifier = if (thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.MOVE_SPEED))) thePlayer.getActivePotionEffect(classProvider.getPotionEnum(PotionType.MOVE_SPEED))!!.amplifier else -1
			thePlayer.motionX *= 0.8
			thePlayer.motionZ *= 0.8
			var moveSpeed = 0.55
			when (amplifier)
			{
				0 -> moveSpeed = 0.85 // 0.31 +6 +6 +
				1 -> moveSpeed = 0.91 // 0.37 - previous value
				2 -> moveSpeed = 1.01 // 0.41
				3 -> moveSpeed = 1.12 // 0.45
				4 -> moveSpeed = 1.23 // 0.49
				5 -> moveSpeed = 1.35 // 0.53
				else ->
				{
				}
			}
			if (thePlayer.onGround)
			{
				event.y = 0.42
				thePlayer.jump()
			}
			event.x = -functions.sin(dir) * moveSpeed
			event.z = functions.cos(dir) * moveSpeed
		}
	}
}
