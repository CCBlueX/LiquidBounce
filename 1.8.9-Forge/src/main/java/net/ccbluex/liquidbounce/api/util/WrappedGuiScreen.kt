/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import java.io.IOException

abstract class WrappedGuiScreen : MinecraftInstance() {
    lateinit var representedScreen: IGuiScreen

    open fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        representedScreen.superDrawScreen(mouseX, mouseY, partialTicks)
    }

    open fun initGui() {
    }

    @Throws(IOException::class)
    open fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        representedScreen.superMouseClicked(mouseX, mouseY, mouseButton)
    }

    open fun updateScreen() {  }

    @Throws(IOException::class)
    open fun handleMouseInput() {
        representedScreen.superHandleMouseInput()
    }

    @Throws(IOException::class)
    open fun keyTyped(typedChar: Char, keyCode: Int) {
        representedScreen.superKeyTyped(typedChar, keyCode)
    }

    @Throws(IOException::class)
    open fun actionPerformed(button: IGuiButton) {
    }

    open fun onGuiClosed() {}
    open fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        representedScreen.superMouseReleased(mouseX, mouseY, state)
    }
    open fun doesGuiPauseGame(): Boolean = false
}