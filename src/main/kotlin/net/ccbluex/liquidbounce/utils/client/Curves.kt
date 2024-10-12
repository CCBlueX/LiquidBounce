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
 */
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.config.NamedChoice
import net.minecraft.util.math.MathHelper.PI
import net.minecraft.util.math.MathHelper.sin
import kotlin.math.pow

/**
 * Not extends (Float) -> Float
 * Avoiding packaged type (java.lang.Float)
 */
sealed interface Curve: NamedChoice {
    /**
     * range = domain = 0F..1F
     */
    operator fun invoke(t: Float): Float
}

enum class Curves(override val choiceName: String) : Curve {
    LINEAR("Linear") {
        override fun invoke(t: Float): Float = t
    },
    EASE_IN("EaseIn") {
        override fun invoke(t: Float): Float = t * t
    },
    EASE_OUT("EaseOut") {
        override fun invoke(t: Float): Float = 1 - (1 - t) * (1 - t)
    },
    EASE_IN_OUT("EaseInOut") {
        override fun invoke(t: Float): Float = 2 * (1 - t) * t * t + t * t
    },
    SINE("Sine") {
        override fun invoke(t: Float): Float = sin(PI * t * 0.5F)
    },
    EXPONENTIAL("Exponential") {
        override fun invoke(t: Float): Float = if (t == 0F) 0F else 2F.pow(10F * (t - 1F))
    },
    CUBIC("Cubic") {
        override fun invoke(t: Float): Float = t * t * t
    },
    ELASTIC("Elastic") {
        override fun invoke(t: Float): Float = sin(6.5F * PI * t) * 2F.pow(10F * (t - 1F))
    },
    BACK("Back") {
        override fun invoke(t: Float): Float = t * t * (2.5F * t - 1.5F)
    };
}
