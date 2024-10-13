package net.ccbluex.liquidbounce.integration.theme.type.native.components

import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.gui.DrawContext

abstract class NativeComponent(theme: Theme, name: String, enabled: Boolean, alignment: Alignment, tweaks: Array<ComponentTweak> = emptyArray()) :
    Component(theme, name, enabled, alignment, tweaks) {

    abstract fun render(context: DrawContext, delta: Float)
    abstract fun size(): Pair<Int, Int>

}
