/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.api.minecraft.util.BlockPos
import net.ccbluex.liquidbounce.event.ClickBlockEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.IntegerValue

@ModuleInfo(name = "AutoTool", description = "Automatically selects the best tool in your inventory to mine a block.", category = ModuleCategory.PLAYER)
class AutoTool : Module()
{
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 1000)

    @EventTarget
    fun onClick(event: ClickBlockEvent)
    {
        switchSlot(event.clickedBlock ?: return)
    }

    fun switchSlot(blockPos: BlockPos)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return
        val inventory = thePlayer.inventory

        val blockState = theWorld.getBlockState(blockPos)
        val currentItemStrVsBlock = inventory.getCurrentItemInHand()?.getStrVsBlock(blockState) ?: 1.0F

        val itemDelay = itemDelayValue.get()
        val currentTime = System.currentTimeMillis()

        // Find the best tool in hotbar
        inventory.currentItem = ((0..8).mapNotNull { it to (inventory.getStackInSlot(it) ?: return@mapNotNull null) }.filter { currentTime - it.second.itemDelay >= itemDelay }.filter { it.second.getStrVsBlock(blockState) > currentItemStrVsBlock }.maxBy { it.second.getStrVsBlock(blockState) } ?: return).first
    }
}
