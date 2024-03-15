package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos

interface GenericColorMode {
    fun getColor(): Color4b
}

class GenericStaticColorMode(
    override val parent: ChoiceConfigurable,
    defaultColor: Color4b
) : Choice("Static"), GenericColorMode {

    val staticColor by color("Color", defaultColor)

    override fun getColor() = staticColor

}

class GenericRainbowColorMode(
    override val parent: ChoiceConfigurable
) : Choice("Rainbow"), GenericColorMode {
    override fun getColor() = rainbow().alpha(50)
}

class MapColorMode(
    override val parent: ChoiceConfigurable
) : Choice("MapColor"), GenericColorMode {

    override fun getColor() = Color4b.WHITE.alpha(100)

    fun getBlockAwareColor(pos: BlockPos, state: BlockState) = Color4b(state.getMapColor(world, pos).color).alpha(100)

}
