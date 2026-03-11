/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
 */

package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.entity.cameraDistance
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.minecraft.core.BlockPos
import net.minecraft.util.ToFloatFunction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector2f

abstract class GenericColorMode<in T>(name: String): Mode(name) {
    /**
     * @return Whether the color mode is sensitive to the parameter of [getColor].
     * If false, it can be used as ColorModulator (shader color)
     */
    open val isParamSensitive: Boolean = true

    abstract fun getColor(param: T): Color4b
}

class GenericStaticColorMode(
    override val parent: ModeValueGroup<*>,
    defaultColor: Color4b
) : GenericColorMode<Any?>("Static") {
    private val staticColor = color("Color", defaultColor)
    override val isParamSensitive: Boolean = false
    override fun getColor(param: Any?) = staticColor.get()
}

class GenericRainbowColorMode(
    override val parent: ModeValueGroup<*>,
    private val alpha: Int = 50
) : GenericColorMode<Any?>("Rainbow") {
    override val isParamSensitive: Boolean = false
    override fun getColor(param: Any?) = rainbow(alpha = alpha / 255f)
}

class MapColorMode(
    override val parent: ModeValueGroup<*>,
    private val alpha: Int = 100
) : GenericColorMode<Pair<BlockPos, BlockState>>("MapColor") {
    override fun getColor(param: Pair<BlockPos, BlockState>): Color4b {
        val (pos, state) = param

        val mapColor = state.getMapColor(world, pos).col
        return Color4b(mapColor).alpha(alpha)
    }
}

class GenericEntityHealthColorMode(
    override val parent: ModeValueGroup<*>
) : GenericColorMode<LivingEntity>("Health") {
    private val alpha by int("Alpha", 255, 0..255)

    override fun getColor(param: LivingEntity): Color4b {
        val maxHealth = param.maxHealth
        val health = param.getActualHealth().coerceAtMost(maxHealth)

        val healthPercentage = health / maxHealth

        val red = (255 * (1 - healthPercentage)).toInt().coerceIn(0..255)
        val green = (255 * healthPercentage).toInt().coerceIn(0..255)

        return Color4b(red, green, 0, alpha)
    }
}

class GenericDistanceHSBColorMode<T : Any>(
    override val parent: ModeValueGroup<*>,
    private val fixedAlpha: Float?,
    private val distanceGetter: ToFloatFunction<T>,
) : GenericColorMode<T>("Distance") {
    private val saturation by float("Saturation", 1F, 0F..1F)
    private val brightness by float("Brightness", 1F, 0F..1F)
    private val hue = curve("Hue") {
        "Distance" x 0f..200f
        "Hue" y 0f..360f
        points(Vector2f(0f, 0f), Vector2f(100f, 120f), Vector2f(200f, 120f))
    }
    private val alphaValue = if (fixedAlpha == null) float("Alpha", 1F, 0F..1F) else null

    override fun getColor(param: T): Color4b {
        val distance = distanceGetter.applyAsFloat(param)
        return Color4b.ofHSB(
            hue = hue.transform(distance) / 360f,
            saturation = saturation,
            brightness = brightness,
            alpha = fixedAlpha ?: alphaValue!!.get(),
        )
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun entity(parent: ModeValueGroup<*>, fixedAlpha: Float? = null) =
            GenericDistanceHSBColorMode<Entity>(parent, fixedAlpha) {
                it.position().cameraDistance().toFloat()
            }
    }
}
