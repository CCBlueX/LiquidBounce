package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class KeepAliveFly : FlyMode("KeepAlive")
{
	override val mark: Boolean
		get() = false

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return
		val gameSettings = mc.gameSettings

		mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketKeepAlive())

		thePlayer.capabilities.isFlying = false

		MovementUtils.zeroXYZ(thePlayer)

		val speed = Fly.baseSpeedValue.get()

		if (gameSettings.keyBindJump.isKeyDown) thePlayer.motionY += speed
		if (gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY -= speed

		MovementUtils.strafe(thePlayer, speed)
	}
}
