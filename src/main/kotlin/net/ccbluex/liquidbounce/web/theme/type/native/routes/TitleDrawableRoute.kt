package net.ccbluex.liquidbounce.web.theme.type.native.routes

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.theme.type.native.NativeDrawableRoute
import net.minecraft.client.gui.DrawContext

class TitleDrawableRoute : NativeDrawableRoute() {

    override fun render(context: DrawContext, delta: Float) {
        context.drawText(mc.textRenderer, "LiquidBounce", 2, 2, 0xFFFFFF, true)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun charTyped(char: Char, modifiers: Int): Boolean {
        TODO("Not yet implemented")
    }

}
