package net.ccbluex.liquidbounce.web.theme.type.native.routes

import net.ccbluex.liquidbounce.web.theme.component.components
import net.ccbluex.liquidbounce.web.theme.type.native.NativeDrawableRoute
import net.ccbluex.liquidbounce.web.theme.type.native.components.NativeComponent
import net.minecraft.client.gui.DrawContext

class HudDrawableRoute : NativeDrawableRoute() {

    override fun render(context: DrawContext, delta: Float) {
        // TODO: Transform with component alignment
        components
            .filterIsInstance<NativeComponent>()
            .forEach { component -> component.render(context, delta) }
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
