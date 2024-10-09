package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.utils.render.Alignment

abstract class ComponentFactory {

    class JsonComponentFactory(
        private val name: String,
        private val alignment: Alignment,
        private val tweaks: Array<ComponentTweak>?
    ) : ComponentFactory() {
        override fun new(theme: Theme) = Component(theme, name, true, alignment, tweaks ?: emptyArray())
    }

    class NativeComponentFactory(
        private val function: () -> Component
    ) : ComponentFactory() {
        override fun new(theme: Theme) = function()
    }

    abstract fun new(theme: Theme): Component

}
