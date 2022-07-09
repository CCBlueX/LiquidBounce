package net.ccbluex.liquidbounce.features.module.modules.movement.flies.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.DamageOnStart
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.multiply
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.zeroXYZ

class BugSpartan : FlyMode("BugSpartan")
{
	override val damageOnStart: DamageOnStart
		get() = DamageOnStart.NCP

	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return

		thePlayer.multiply(0.1)
		thePlayer.swingItem()
	}

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return
		val gameSettings = mc.gameSettings

		val speed = Fly.baseSpeedValue.get()

		thePlayer.capabilities.isFlying = false

		thePlayer.zeroXYZ()

		if (gameSettings.keyBindJump.isKeyDown) thePlayer.motionY += speed
		if (gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY -= speed

		thePlayer.strafe(speed)
	}
}
