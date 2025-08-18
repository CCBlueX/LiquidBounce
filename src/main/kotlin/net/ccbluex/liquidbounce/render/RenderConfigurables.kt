package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.minecraft.block.BlockState
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.BlockPos

abstract class GenericColorMode<in T>(name: String): Choice(name) {
    abstract fun getColor(param: T): Color4b
}

class GenericStaticColorMode(
    override val parent: ChoiceConfigurable<*>,
    defaultColor: Color4b
) : GenericColorMode<Any?>("Static") {

    private val staticColor by color("Color", defaultColor)

    override fun getColor(param: Any?) = staticColor

}

class GenericRainbowColorMode(
    override val parent: ChoiceConfigurable<*>,
    private val alpha: Int = 50
) : GenericColorMode<Any?>("Rainbow") {
    override fun getColor(param: Any?) = rainbow().with(a = alpha)
}

class MapColorMode(
    override val parent: ChoiceConfigurable<*>,
    private val alpha: Int = 100
) : GenericColorMode<Pair<BlockPos, BlockState>>("MapColor") {

    override fun getColor(param: Pair<BlockPos, BlockState>): Color4b {
        val (pos, state) = param

        return Color4b(state.getMapColor(world, pos).color).with(a = alpha)
    }

}


class GenericEntityHealthColorMode(
    override val parent: ChoiceConfigurable<*>
) : GenericColorMode<LivingEntity>("Health") {
    override fun getColor(param: LivingEntity): Color4b {
        val maxHealth = param.maxHealth
        val health = param.getActualHealth().coerceAtMost(maxHealth)

        val healthPercentage = health / maxHealth

        val red = (255 * (1 - healthPercentage)).toInt().coerceIn(0..255)
        val green = (255 * healthPercentage).toInt().coerceIn(0..255)

        return Color4b(red, green, 0)
    }
}
