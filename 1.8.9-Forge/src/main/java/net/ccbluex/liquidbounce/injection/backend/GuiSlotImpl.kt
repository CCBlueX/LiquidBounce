/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiSlot
import net.ccbluex.liquidbounce.injection.backend.utils.GuiSlotWrapper
import net.ccbluex.liquidbounce.injection.implementations.IMixinGuiSlot
import net.minecraft.client.gui.GuiSlot

class GuiSlotImpl(val wrapped: GuiSlot) : IGuiSlot {
    override val width: Int
        get() = wrapped.width
    override val slotHeight: Int
        get() = wrapped.slotHeight

    override fun scrollBy(value: Int) = wrapped.scrollBy(value)

    override fun registerScrollButtons(down: Int, up: Int) = wrapped.registerScrollButtons(down, up)

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) = wrapped.drawScreen(mouseX, mouseY, partialTicks)

    override fun elementClicked(index: Int, doubleClick: Boolean, var3: Int, var4: Int) = (wrapped as GuiSlotWrapper).elementClicked(index, doubleClick, var3, var4)

    override fun handleMouseInput() = wrapped.handleMouseInput()
    override fun setListWidth(width: Int) = (wrapped as IMixinGuiSlot).setListWidth(width)

    override fun setEnableScissor(flag: Boolean) = (wrapped as IMixinGuiSlot).setEnableScissor(flag)

}

inline fun IGuiSlot.unwrap(): GuiSlot = (this as GuiSlotImpl).wrapped
inline fun GuiSlot.wrap(): IGuiSlot = GuiSlotImpl(this)