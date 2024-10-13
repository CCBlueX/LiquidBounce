package net.ccbluex.liquidbounce.integration.theme.type.native

import net.minecraft.client.gui.DrawContext

abstract class NativeDrawableRoute {

    abstract fun render(context: DrawContext, delta: Float)

    open fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) = false
    open fun mouseReleased(mouseX: Double, mouseY: Double, mouseButton: Int) = false
    open fun mouseMoved(mouseX: Double, mouseY: Double) {}
    open fun mouseScrolled(mouseX: Double, mouseY: Double) {}
    open fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) = false
    open fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int) = false
    open fun charTyped(char: Char, modifiers: Int) = false

}
