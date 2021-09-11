package net.ccbluex.liquidbounce.features.module.modules.movement.flies.vanilla

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class VanillaFly : FlyMode("Vanilla")
{
	override val mark: Boolean
		get() = false

	override fun onUpdate()
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val gameSettings = mc.gameSettings

		val speed = Fly.baseSpeedValue.get()

		thePlayer.capabilities.isFlying = false

		MovementUtils.zeroXYZ(thePlayer)

		if (gameSettings.keyBindJump.isKeyDown) thePlayer.motionY += speed
		if (gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY -= speed

		MovementUtils.strafe(thePlayer, speed)

		handleVanillaKickBypass(theWorld, thePlayer)
	}
}
