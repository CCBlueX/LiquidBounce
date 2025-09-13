package net.ccbluex.liquidbounce.integration.theme.component.components.notification

import net.ccbluex.liquidbounce.integration.theme.component.components.NativeComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.notification.mode.NovolineMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment

object NotificationComponent: NativeComponent(
    "Notification", true, Alignment(
        horizontalAlignment = Alignment.ScreenAxisX.RIGHT,
        horizontalOffset = 50,
        verticalAlignment = Alignment.ScreenAxisY.BOTTOM,
        verticalOffset = 50,
    )
)  {
    init {
        registerComponentListen(this)
    }
    val modes = choices(this, "Mode", NovolineMode, arrayOf(NovolineMode))
    val size by float("Size", 1f, 0.5f..1f)
    val backgroundColor by color("Background", Color4b.DARK_GRAY.withAlpha(125))
    override fun onEnabled() {
        modes.activeChoice.enable()
    }

    override fun onDisabled() {
        modes.activeChoice.disable()
    }

}
