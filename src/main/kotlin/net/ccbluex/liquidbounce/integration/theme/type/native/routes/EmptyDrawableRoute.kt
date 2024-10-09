package net.ccbluex.liquidbounce.integration.theme.type.native.routes

import net.ccbluex.liquidbounce.integration.theme.type.native.NativeDrawableRoute
import net.minecraft.client.gui.DrawContext

class EmptyDrawableRoute : NativeDrawableRoute() {
    override fun render(context: DrawContext, delta: Float) { }
}
