/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.FloatValue

@ModuleInfo(name = "WaterSpeed", description = "Allows you to swim faster. (bypassed ~AAC3.2.2)", category = ModuleCategory.MOVEMENT)
class WaterSpeed : Module()
{
	private val speedValue = FloatValue("Speed", 1.2f, 1.1f, 1.5f)

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (thePlayer.isInWater && classProvider.isBlockLiquid(getBlock(theWorld, thePlayer.position))) MovementUtils.multiply(thePlayer, speedValue.get())
	}

	override val tag: String
		get() = "${speedValue.get()}"
}
