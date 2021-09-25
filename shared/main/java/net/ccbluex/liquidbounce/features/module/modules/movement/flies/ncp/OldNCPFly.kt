package net.ccbluex.liquidbounce.features.module.modules.movement.flies.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.DamageOnStart
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.strafe

class OldNCPFly : FlyMode("OldNCP")
{
	override val damageOnStart: DamageOnStart
		get() = DamageOnStart.OLD_NCP

	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return

		if (!thePlayer.onGround) return

		thePlayer.jump()
		thePlayer.swingItem()
	}

	override fun onUpdate()
	{
		val thePlayer = mc.thePlayer ?: return
		val gameSettings = mc.gameSettings

		val posY = thePlayer.posY

		if (Fly.startY > posY) thePlayer.motionY = -0.000000000000000000000000000000001
		if (gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -0.2
		if (gameSettings.keyBindJump.isKeyDown && posY < Fly.startY - 0.1) thePlayer.motionY = 0.2

		thePlayer.strafe()
	}
}
