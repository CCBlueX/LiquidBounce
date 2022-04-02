/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.utils

import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.injection.backend.wrap
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen

class GuiScreenWrapper(val wrapped: WrappedGuiScreen): GuiScreen() {
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) = wrapped.drawScreen(mouseX, mouseY, partialTicks)
    override fun initGui() = wrapped.initGui()
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) = wrapped.mouseClicked(mouseX, mouseY, mouseButton)
    override fun updateScreen() = wrapped.updateScreen()

    override fun handleMouseInput() = wrapped.handleMouseInput()

    override fun keyTyped(typedChar: Char, keyCode: Int) = wrapped.keyTyped(typedChar, keyCode)

    override fun actionPerformed(button: GuiButton?) {
        button?.let { wrapped.actionPerformed(button.wrap()) }
    }

    override fun onGuiClosed() = wrapped.onGuiClosed()

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) = wrapped.mouseReleased(mouseX, mouseY, state)

    override fun doesGuiPauseGame(): Boolean = wrapped.doesGuiPauseGame()

    fun superMouseReleased(mouseX: Int, mouseY: Int, state: Int) = super.mouseReleased(mouseX, mouseY, state)
    fun superKeyTyped(typedChar: Char, keyCode: Int) = super.keyTyped(typedChar, keyCode)
    fun superHandleMouseInput() = super.handleMouseInput()
    fun superMouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) = super.mouseClicked(mouseX, mouseY, mouseButton)
    fun superDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) = super.drawScreen(mouseX, mouseY, partialTicks)
}