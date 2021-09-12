package net.ccbluex.liquidbounce.features.module.modules.movement.flies.minorACs

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer

class MineSecureGlide : FlyMode("MineSecureGlide")
{
	private val mineSecureVClipTimer = MSTimer()

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return
		val networkManager = mc.netHandler.networkManager
		val gameSettings = mc.gameSettings

		thePlayer.capabilities.isFlying = false

		if (!gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -0.01

		MovementUtils.zeroXZ(thePlayer)

		MovementUtils.strafe(thePlayer, Fly.baseSpeedValue.get())

		if (mineSecureVClipTimer.hasTimePassed(150) && gameSettings.keyBindJump.isKeyDown)
		{
			networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(thePlayer.posX, thePlayer.posY + 5, thePlayer.posZ, false))
			networkManager.sendPacketWithoutEvent(classProvider.createCPacketPlayerPosition(0.5, -1000.0, 0.5, false))

			MovementUtils.forward(thePlayer, 0.4)

			mineSecureVClipTimer.reset()
		}
	}
}
