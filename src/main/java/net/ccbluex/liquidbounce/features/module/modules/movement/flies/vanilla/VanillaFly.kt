package net.ccbluex.liquidbounce.features.module.modules.movement.flies.vanilla

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.zeroXYZ

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

		thePlayer.zeroXYZ()

		if (gameSettings.keyBindJump.isKeyDown) thePlayer.motionY += speed
		if (gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY -= speed

		thePlayer.strafe(speed)

		handleVanillaKickBypass(theWorld, thePlayer)
	}
}
