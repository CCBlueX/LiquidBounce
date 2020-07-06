package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiSlot
import net.minecraft.client.gui.GuiSlot

class GuiSlotImpl(val wrapped: GuiSlot) : IGuiSlot {
    override val slotHeight: Int
        get() = wrapped.slotHeight

    override fun scrollBy(value: Int) = wrapped.scrollBy(value)

    override fun registerScrollButtons(down: Int, up: Int) = wrapped.registerScrollButtons(down, up)

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) = wrapped.drawScreen(mouseX, mouseY, partialTicks)

    override fun elementClicked(index: Int, doubleClick: Boolean, var3: Int, var4: Int) = wrapped.elementClicked(index, doubleClick, var3, var4)

    override fun handleMouseInput() = wrapped.handleMouseInput()

}

inline fun IGuiSlot.unwrap(): GuiSlot = (this as GuiSlotImpl).wrapped
inline fun GuiSlot.wrap(): IGuiSlot = GuiSlotImpl(this)