/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.ClickBlockEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo

@ModuleInfo(name = "AutoTool", description = "Automatically selects the best tool in your inventory to mine a block.", category = ModuleCategory.PLAYER)
class AutoTool : Module()
{

	@EventTarget
	fun onClick(event: ClickBlockEvent)
	{
		switchSlot(event.clickedBlock ?: return)
	}

	fun switchSlot(blockPos: WBlockPos)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val blockState = theWorld.getBlockState(blockPos)

		// Find the best tool in hotbar
		thePlayer.inventory.currentItem = (0..8).filterNot { thePlayer.inventory.getStackInSlot(it) == null }.maxBy { thePlayer.inventory.getStackInSlot(it)!!.getStrVsBlock(blockState) } ?: return
	}
}
