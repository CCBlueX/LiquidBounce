package net.ccbluex.liquidbounce.integration.theme.type.native.components

import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.minecraft.client.gui.DrawContext

abstract class NativeComponent(theme: Theme, name: String, enabled: Boolean, tweaks: Array<ComponentTweak> = emptyArray()) :
    Component(theme, name, enabled, tweaks) {

    abstract fun render(context: DrawContext, delta: Float)

}
