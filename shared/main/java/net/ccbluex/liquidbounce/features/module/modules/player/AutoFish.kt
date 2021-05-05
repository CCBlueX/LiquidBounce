/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.api.MinecraftVersion
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.timer.MSTimer

@ModuleInfo(name = "AutoFish", description = "Automatically catches fish when using a rod.", category = ModuleCategory.PLAYER, supportedVersions = [MinecraftVersion.MC_1_8])
class AutoFish : Module()
{
	private val rodOutTimer = MSTimer()

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.heldItem == null || !classProvider.isItemFishingRod(thePlayer.heldItem?.item)) return

		val fishEntity = thePlayer.fishEntity

		if (rodOutTimer.hasTimePassed(500L) && fishEntity == null || (fishEntity != null && fishEntity.motionX == 0.0 && fishEntity.motionZ == 0.0 && fishEntity.motionY != 0.0))
		{
			mc.rightClickMouse()
			rodOutTimer.reset()
		}
	}
}
