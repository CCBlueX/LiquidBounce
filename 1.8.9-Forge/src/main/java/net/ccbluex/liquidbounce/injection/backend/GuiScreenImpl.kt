/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiGameOver
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.minecraft.client.gui.inventory.IGuiChest
import net.ccbluex.liquidbounce.api.minecraft.client.gui.inventory.IGuiContainer
import net.ccbluex.liquidbounce.api.util.WrappedMutableList
import net.ccbluex.liquidbounce.injection.backend.utils.GuiScreenWrapper
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiGameOver
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer

open class GuiScreenImpl<T : GuiScreen>(wrapped: T) : GuiImpl<T>(wrapped), IGuiScreen {
    override val fontRendererObj: IFontRenderer
        get() = wrapped.fontRendererObj.wrap()
    override val buttonList: MutableList<IGuiButton>
        get() = WrappedMutableList(wrapped.buttonList, IGuiButton::unwrap, GuiButton::wrap)

    override fun asGuiContainer(): IGuiContainer = GuiContainerImpl(wrapped as GuiContainer)

    override fun asGuiGameOver(): IGuiGameOver = GuiGameOverImpl(wrapped as GuiGameOver)

    override fun asGuiChest(): IGuiChest = GuiChestImpl(wrapped as GuiChest)

    override fun superMouseReleased(mouseX: Int, mouseY: Int, state: Int) = (wrapped as GuiScreenWrapper).superMouseReleased(mouseX, mouseY, state)

    override fun drawBackground(i: Int) = wrapped.drawBackground(i)

    override fun drawDefaultBackground() = wrapped.drawDefaultBackground()
    override fun superKeyTyped(typedChar: Char, keyCode: Int) = (wrapped as GuiScreenWrapper).superKeyTyped(typedChar, keyCode)

    override fun superHandleMouseInput() = (wrapped as GuiScreenWrapper).superHandleMouseInput()

    override fun superMouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) = (wrapped as GuiScreenWrapper).superMouseClicked(mouseX, mouseY, mouseButton)
    override fun superDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) = (wrapped as GuiScreenWrapper).superDrawScreen(mouseX, mouseY, partialTicks)

    override var height: Int
        get() = wrapped.height
        set(value) {
            wrapped.height = value
        }
    override var width: Int
        get() = wrapped.width
        set(value) {
            wrapped.width = value
        }

    override fun equals(other: Any?): Boolean {
        return other is GuiScreenImpl<*> && other.wrapped == this.wrapped
    }
}

inline fun IGuiScreen.unwrap(): GuiScreen = (this as GuiScreenImpl<*>).wrapped
inline fun GuiScreen.wrap(): IGuiScreen = GuiScreenImpl(this)