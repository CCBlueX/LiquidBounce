package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.vanilla

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
class Vanilla : SpeedMode("Vanilla")
{
	override fun onMotion()
	{
		strafe((LiquidBounce.moduleManager[Speed::class.java] as Speed).vanillaSpeedValue.get())
	}

	override fun onUpdate()
	{
	}

	override fun onMove(event: MoveEvent)
	{
	}
}
