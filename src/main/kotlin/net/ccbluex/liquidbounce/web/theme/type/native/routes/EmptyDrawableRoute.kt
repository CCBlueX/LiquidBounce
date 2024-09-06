package net.ccbluex.liquidbounce.web.theme.type.native.routes

import net.ccbluex.liquidbounce.web.theme.type.native.NativeDrawableRoute
import net.minecraft.client.gui.DrawContext

class EmptyDrawableRoute : NativeDrawableRoute() {

    override fun render(context: DrawContext, delta: Float) {

    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return true
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return true
    }

    override fun charTyped(char: Char, modifiers: Int): Boolean {
        return true
    }

}
