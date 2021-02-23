/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo

@ModuleInfo(name = "AutoBreak", description = "Automatically breaks the block you are looking at.", category = ModuleCategory.WORLD)
class AutoBreak : Module()
{

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val theWorld = mc.theWorld ?: return
		val mouseOverPos = mc.objectMouseOver?.blockPos ?: return

		mc.gameSettings.keyBindAttack.pressed = theWorld.getBlockState(mouseOverPos).block != classProvider.getBlockEnum(BlockType.AIR)
	}

	override fun onDisable()
	{
		val gameSettings = mc.gameSettings

		if (!gameSettings.isKeyDown(gameSettings.keyBindAttack)) gameSettings.keyBindAttack.pressed = false
	}
}
