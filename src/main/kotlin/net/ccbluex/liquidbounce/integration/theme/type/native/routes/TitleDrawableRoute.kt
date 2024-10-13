package net.ccbluex.liquidbounce.integration.theme.type.native.routes

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.integration.theme.type.native.NativeDrawableRoute
import net.minecraft.client.gui.DrawContext

class TitleDrawableRoute : NativeDrawableRoute() {

    override fun render(context: DrawContext, delta: Float) {
        context.drawText(mc.textRenderer, "LiquidBounce", 2, 2, 0xFFFFFF, true)
    }

}
