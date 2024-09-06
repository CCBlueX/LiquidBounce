package net.ccbluex.liquidbounce.integration.theme.type.native.components

import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.minecraft.client.gui.DrawContext

class ArrayListNativeComponent(
    theme: Theme,
) : NativeComponent(theme, "ArrayList", true) {
    override fun render(context: DrawContext, delta: Float) {
        val x = 2
        var y = 2

        for (module in ModuleManager) {
            if (module.hidden || !module.enabled) {
                continue
            }

            val text = module.name.asText()
            context.drawText(mc.textRenderer, text, x, y, 0xFFFFFF, true)
            y += 10
        }
    }

}
