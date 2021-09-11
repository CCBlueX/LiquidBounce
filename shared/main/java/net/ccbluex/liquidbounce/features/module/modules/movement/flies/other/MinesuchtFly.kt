package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.timer.MSTimer

class MinesuchtFly : FlyMode("Minesucht")
{
	private val teleportTimer = MSTimer()

	override fun onUpdate()
	{
		if (!mc.gameSettings.keyBindForward.isKeyDown) return

		val thePlayer = mc.thePlayer ?: return
		val networkManager = mc.netHandler.networkManager
		val provider = classProvider

		val x = thePlayer.posX
		val y = thePlayer.posY
		val z = thePlayer.posZ

		if (teleportTimer.hasTimePassed(99))
		{
			val vec3: WVec3 = thePlayer.getPositionEyes(0.0f)
			val vec31: WVec3 = thePlayer.getLook(0.0f)
			val vec32: WVec3 = vec3.addVector(vec31.xCoord * 7, vec31.yCoord * 7, vec31.zCoord * 7)

			if (thePlayer.fallDistance > 0.8)
			{
				networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y + 50, z, false))

				thePlayer.fall(100.0f, 100.0f)
				thePlayer.fallDistance = 0.0f

				networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y + 20, z, true))
			}

			networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(vec32.xCoord, y + 50, vec32.zCoord, true))
			networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y, z, false))
			networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(vec32.xCoord, y, vec32.zCoord, true))
			networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y, z, false))

			teleportTimer.reset()
		}
		else
		{
			networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y, z, false))
			networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y, z, true))
		}
	}
}
