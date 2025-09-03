package net.ccbluex.liquidbounce.integration.theme.component.components.targethud

import net.ccbluex.liquidbounce.integration.theme.component.components.NativeComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.targethud.mode.NovolineMode
import net.ccbluex.liquidbounce.render.GenericCustomColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment

object TargetHudComponent : NativeComponent(
    "TargetHud", true, Alignment(
        horizontalAlignment = Alignment.ScreenAxisX.CENTER,
        horizontalOffset = 100,
        verticalAlignment = Alignment.ScreenAxisY.CENTER_TRANSLATED,
        verticalOffset = 50,
    )
) {
    init {
        registerComponentListen(this)
    }

    val modes = choices(this, "Mode", NovolineMode, arrayOf(NovolineMode))

    val colorModes = choices(this, "ColorMode", 2) {
        arrayOf(
            GenericCustomColorMode(it, Color4b.RED.with(a = 137), Color4b.RED.with(a = 233)),
            GenericStaticColorMode(it, Color4b.RED.with(a = 150)),
            GenericSyncColorMode(it)
        )
    }
    val backgroundColor by color("Background", Color4b.DARK_GRAY.withAlpha(125))
    val borderColor by color("Border", Color4b.TRANSPARENT)
    val textColor by color("Name", Color4b.WHITE)
    val size by float("Size", 1f, 0.8f..1.5f)

    fun applyAdaptiveScale(baseW: Float, baseH: Float, block: (scale: Float, cx: Float, cy: Float) -> Unit) {
        val window = mc.window
        val s = size.coerceAtLeast(0.1f)
        val scale = (window.scaledWidth.coerceAtMost(window.scaledHeight)) / 500f * s

        val bounds = alignment.getBounds(baseW * scale, baseH * scale)
        val cx = bounds.xMin + (baseW * scale) / 2f
        val cy = bounds.yMin + (baseH * scale) / 2f

        block(scale, cx, cy)
    }

    override fun onEnabled() {
        modes.activeChoice.enable()
    }

    override fun onDisabled() {
        modes.activeChoice.disable()
    }

}
