package net.ccbluex.liquidbounce.integration.theme.type.native.components

import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.liquidbounce.utils.render.Alignment.ScreenAxisX
import net.ccbluex.liquidbounce.utils.render.Alignment.ScreenAxisY
import net.minecraft.client.gui.DrawContext
import kotlin.math.max

class ArrayListNativeComponent(
    theme: Theme,
) : NativeComponent(theme, "ArrayList", true, Alignment(
    ScreenAxisX.RIGHT,
    2,
    ScreenAxisY.TOP,
    2
)) {

    override fun render(context: DrawContext, delta: Float) {
        val textRenderer = mc.textRenderer

        ModuleManager.filter { m -> m.enabled && !m.hidden }.forEachIndexed { index, module ->
            val text = module.name.asText()
            context.drawText(textRenderer, text, 0, index * textRenderer.fontHeight, 0xFFFFFF,
                true)
        }
    }

    override fun size(): Pair<Int, Int> {
        val moduleList = ModuleManager.filter { m -> m.enabled && !m.hidden }

        return max(100, moduleList.maxOf { module -> mc.textRenderer.getWidth(module.name) }) to
            max(50, moduleList.size * mc.textRenderer.fontHeight)
    }

}
