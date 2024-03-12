package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.utils.rainbow

interface GenericColorMode<T> {
    fun getColor(param: T? = null): Color4b
}

class GenericStaticColorMode(
    override val parent: ChoiceConfigurable,
    defaultColor: Color4b
) : Choice("Static"), GenericColorMode<Any> {
    val color by color("Color", defaultColor)

    override fun getColor(param: Any?): Color4b {
        return color
    }
}

class GenericRainbowColorMode(
    override val parent: ChoiceConfigurable
) : Choice("Rainbow"), GenericColorMode<Any> {
    override fun getColor(param: Any?): Color4b {
        return rainbow().alpha(50)
    }
}
