package net.ccbluex.liquidbounce.features.module.modules.movement.flies.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.timer.TickTimer

class Spartan185Fly : FlyMode("Spartan185")
{
	private val spartanTimer = TickTimer()

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return
		val networkManager = mc.netHandler.networkManager

		val x = thePlayer.posX
		val y = thePlayer.posY
		val z = thePlayer.posZ

		thePlayer.motionY = 0.0

		spartanTimer.update()
		if (spartanTimer.hasTimePassed(12))
		{
			networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(x, y + 8, z, true))
			networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(x, y - 8, z, true))
			spartanTimer.reset()
		}
	}
}
