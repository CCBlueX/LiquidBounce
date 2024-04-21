/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.utils.rainbow
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
    override fun getColor(param: Any?) = rainbow().alpha(alpha)
}

class MapColorMode(
    override val parent: ChoiceConfigurable<*>,
    private val alpha: Int = 100
) : GenericColorMode<Pair<BlockPos, BlockState>>("MapColor") {

    override fun getColor(param: Pair<BlockPos, BlockState>): Color4b {
        val (pos, state) = param

        return Color4b(state.getMapColor(world, pos).color).alpha(alpha)
    }

}


class GenericEntityHealthColorMode(
    override val parent: ChoiceConfigurable<*>
) : GenericColorMode<LivingEntity>("Health") {
    override fun getColor(param: LivingEntity): Color4b {
        val health = param.health
        val maxHealth = param.maxHealth

        val healthPercentage = health / maxHealth

        val red = (255 * (1 - healthPercentage)).toInt().coerceIn(0..255)
        val green = (255 * healthPercentage).toInt().coerceIn(0..255)

        return Color4b(red, green, 0)
    }
}
