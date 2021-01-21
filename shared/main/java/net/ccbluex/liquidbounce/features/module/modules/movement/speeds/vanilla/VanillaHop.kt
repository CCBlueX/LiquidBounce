package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.vanilla

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
class VanillaHop : SpeedMode("VanillaHop")
{
	override fun onMotion()
	{
		val thePlayer = mc.thePlayer ?: return

		if (isMoving)
		{
			if (thePlayer.onGround) thePlayer.jump()
			strafe((LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed).vanillaSpeedValue.get())
		} else
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
