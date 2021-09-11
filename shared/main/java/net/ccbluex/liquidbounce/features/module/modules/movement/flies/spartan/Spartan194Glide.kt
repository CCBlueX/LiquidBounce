package net.ccbluex.liquidbounce.features.module.modules.movement.flies.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.TickTimer

class Spartan194Glide : FlyMode("Spartan194Glide")
{
	val spartanTimer = TickTimer()

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return
		val networkManager = mc.netHandler.networkManager

		val x = thePlayer.posX
		val y = thePlayer.posY
		val z = thePlayer.posZ

		MovementUtils.strafe(thePlayer, 0.264f)

		if (thePlayer.ticksExisted % 8 == 0) networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(x, y + 10, z, true))
	}
}
