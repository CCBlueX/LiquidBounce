package net.ccbluex.liquidbounce.injection.backend.utils

import net.ccbluex.liquidbounce.api.minecraft.client.IMinecraft
import net.ccbluex.liquidbounce.api.util.WrappedGuiSlot
import net.ccbluex.liquidbounce.injection.backend.GuiSlotImpl
import net.ccbluex.liquidbounce.injection.backend.unwrap
import net.minecraft.client.gui.GuiSlot

class GuiSlotWrapper(val wrapped: WrappedGuiSlot, mc: IMinecraft, width: Int, height: Int, topIn: Int, bottomIn: Int, slotHeightIn: Int): GuiSlot(mc.unwrap(), width, height, topIn, bottomIn, slotHeightIn) {

    init {
        wrapped.represented = GuiSlotImpl(this)
    }

    override fun getSize(): Int = wrapped.getSize()

    override fun drawSlot(entryID: Int, p_180791_2_: Int, p_180791_3_: Int, p_180791_4_: Int, mouseXIn: Int, mouseYIn: Int) = wrapped.drawSlot(entryID, p_180791_2_, p_180791_3_, p_180791_4_, mouseXIn, mouseYIn)

    override fun isSelected(slotIndex: Int): Boolean = wrapped.isSelected(slotIndex)

    override fun drawBackground() = wrapped.drawBackground()

    public override fun elementClicked(slotIndex: Int, isDoubleClick: Boolean, mouseX: Int, mouseY: Int) = wrapped.elementClicked(slotIndex, isDoubleClick, mouseX, mouseY)
}