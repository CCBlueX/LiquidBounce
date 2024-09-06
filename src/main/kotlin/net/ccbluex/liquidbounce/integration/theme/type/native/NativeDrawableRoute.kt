package net.ccbluex.liquidbounce.integration.theme.type.native

import net.minecraft.client.gui.DrawContext

abstract class NativeDrawableRoute  {

    abstract fun render(context: DrawContext, delta: Float)

    abstract fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean
    abstract fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean
    abstract fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean
    abstract fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean
    abstract fun charTyped(char: Char, modifiers: Int): Boolean

}
