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
            GenericSyncColorMode(it, defaultEndAlpha = 137, defaultStartAlpha = 150)
        )
    }
    val backgroundColor by color("Background", Color4b.DARK_GRAY.withAlpha(125))
    val borderColor by color("Border", Color4b.TRANSPARENT)
    val textColor by color("Name", Color4b.WHITE)
    val size by float("Size", 1f, 0.8f..1.5f)


    override fun onEnabled() {
        modes.activeChoice.enable()
    }

    override fun onDisabled() {
        modes.activeChoice.disable()
    }

}
