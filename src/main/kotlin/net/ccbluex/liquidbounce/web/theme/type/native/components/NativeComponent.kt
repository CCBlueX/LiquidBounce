package net.ccbluex.liquidbounce.web.theme.type.native.components

import net.ccbluex.liquidbounce.web.theme.component.Component
import net.ccbluex.liquidbounce.web.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.web.theme.type.Theme
import net.minecraft.client.gui.DrawContext

abstract class NativeComponent(theme: Theme, name: String, enabled: Boolean, tweaks: Array<ComponentTweak> = emptyArray()) :
    Component(theme, name, enabled, tweaks) {

    abstract fun render(context: DrawContext, delta: Float)

}
