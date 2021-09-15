package net.ccbluex.liquidbounce.features.module.modules.movement.flies.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.DamageOnStart
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class BugSpartan : FlyMode("BugSpartan")
{
	override val damageOnStart: DamageOnStart
		get() = DamageOnStart.NCP

	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return

		MovementUtils.multiply(thePlayer, 0.1)
		thePlayer.swingItem()
	}

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return
		val gameSettings = mc.gameSettings

		val speed = Fly.baseSpeedValue.get()

		thePlayer.capabilities.isFlying = false

		MovementUtils.zeroXYZ(thePlayer)

		if (gameSettings.keyBindJump.isKeyDown) thePlayer.motionY += speed
		if (gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY -= speed

		MovementUtils.strafe(thePlayer, speed)
	}
}
