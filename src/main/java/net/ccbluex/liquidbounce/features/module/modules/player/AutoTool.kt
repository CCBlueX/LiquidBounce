/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.ClickBlockEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.render.FakeItemRender
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.util.BlockPos

object AutoTool :
    Module("AutoTool", ModuleCategory.PLAYER, subjective = true, gameDetecting = false, hideModule = false) {

    private val fakeItem by BoolValue("FakeItem", false)
    private val switchBack by BoolValue("SwitchBack", false)

    @EventTarget
    fun onClick(event: ClickBlockEvent) {
        switchSlot(event.clickedBlock ?: return)
    }

    var formerSlot = -1;

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // set fakeItem to null if mouse is not pressed
        if (!mc.gameSettings.keyBindAttack.isKeyDown) {
            if (switchBack && formerSlot != -1) {
                mc.thePlayer.inventory.currentItem = formerSlot
                formerSlot = -1
            }
            FakeItemRender.fakeItem = -1
        }
    }

    fun switchSlot(blockPos: BlockPos) {
        var bestSpeed = 1F
        var bestSlot = -1

        val blockState = mc.theWorld.getBlockState(blockPos)

        for (i in 0..8) {
            val item = mc.thePlayer.inventory.getStackInSlot(i) ?: continue
            val speed = item.getStrVsBlock(blockState.block)

            if (speed > bestSpeed) {
                bestSpeed = speed
                bestSlot = i
            }
        }

        if (bestSlot != -1 && mc.thePlayer.inventory.currentItem != bestSlot) {
            if (fakeItem && FakeItemRender.fakeItem == -1) {
                FakeItemRender.fakeItem = mc.thePlayer.inventory.currentItem
            }
            if (formerSlot == -1) {
                formerSlot = mc.thePlayer.inventory.currentItem
            }
            mc.thePlayer.inventory.currentItem = bestSlot
        }

    }

}