package net.ccbluex.liquidbounce.integration.theme.component.components.targethud

import net.ccbluex.liquidbounce.integration.theme.component.components.NativeComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.targethud.mode.NovolineMode
import net.ccbluex.liquidbounce.render.GenericCustomColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment

object TargetHudComponent  : NativeComponent("TargetHud", true, Alignment(
    horizontalAlignment = Alignment.ScreenAxisX.LEFT,
    horizontalOffset = 7,
    verticalAlignment = Alignment.ScreenAxisY.TOP,
    verticalOffset = 180,
)) {
    init {
        registerComponentListen(this)
    }
    val modes = choices(this,"Mode", NovolineMode, arrayOf(NovolineMode))

    val colorModes = choices(this, "ColorMode", 2) {
        arrayOf(
            GenericCustomColorMode(it, Color4b.RED.with(a = 137), Color4b.RED.with(a = 233)),
            GenericStaticColorMode(it, Color4b.RED.with(a = 150)),
            GenericSyncColorMode(it)
        )
    }
    val xOffsetRatio by float("X-Offset", 0.55f, 0f..1f)
    val yOffsetRatio by float("Y-Offset", 0.6f, 0f..1f)
    val backgroundColor by color("Background", Color4b.DARK_GRAY.withAlpha(125))
    val borderColor by color("Border", Color4b.TRANSPARENT)
    val textColor by color("Name", Color4b.WHITE)

    override fun onEnabled() {
        modes.activeChoice.enable()
    }

    override fun onDisabled() {
        modes.activeChoice.disable()
    }


}
