package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.timer.TickTimer

class CubeCraftGlide : FlyMode("CubeCraft")
{
	private val cubecraftTeleportTickTimer = TickTimer()

	override fun onUpdate()
	{
		val timer = mc.timer

		timer.timerSpeed = 0.6f
		cubecraftTeleportTickTimer.update()
	}

	override fun onMove(event: MoveEvent)
	{
		val yaw = WMathHelper.toRadians((mc.thePlayer ?: return).rotationYaw)
		val func = functions

		if (cubecraftTeleportTickTimer.hasTimePassed(2))
		{
			event.x = -func.sin(yaw) * 2.4
			event.z = func.cos(yaw) * 2.4

			cubecraftTeleportTickTimer.reset()
		}
		else
		{
			event.x = -func.sin(yaw) * 0.2
			event.z = func.cos(yaw) * 0.2
		}
	}
}
