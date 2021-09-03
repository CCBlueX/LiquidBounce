/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils

@ModuleInfo(name = "NoClip", description = "Allows you to freely move through walls (A sandblock has to fall on your head).", category = ModuleCategory.MOVEMENT)
class NoClip : Module()
{

	override fun onDisable()
	{
		mc.thePlayer?.noClip = false
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val gameSettings = mc.gameSettings

		thePlayer.noClip = true
		thePlayer.fallDistance = 0f
		thePlayer.onGround = false

		thePlayer.capabilities.isFlying = false

		MovementUtils.zeroXYZ(thePlayer)

		val fly = LiquidBounce.moduleManager[Fly::class.java] as Fly
		val speed = fly.baseSpeedValue.get()

		thePlayer.jumpMovementFactor = speed

		if (gameSettings.keyBindJump.isKeyDown) thePlayer.motionY += speed.toDouble()

		if (gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY -= speed.toDouble()
	}
}
