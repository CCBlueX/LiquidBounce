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
package net.ccbluex.liquidbounce.utils.math

import net.ccbluex.liquidbounce.config.types.list.Tagged
import kotlin.math.pow

/**
 * Functions from https://easings.net.
 */
@Suppress("unused")
enum class Easing(override val tag: String) : Tagged {

    LINEAR("Linear") {
        override fun transform(x: Float) = x
    },
    QUAD_IN("QuadIn") {
        override fun transform(x: Float) = x * x
    },
    QUAD_OUT("QuadOut") {
        override fun transform(x: Float) = 1 - (1 - x) * (1 - x)
    },
    QUAD_IN_OUT("QuadInOut") {
        override fun transform(x: Float) = 2 * (1 - x) * x * x + x * x
    },
    EXPONENTIAL_IN("ExponentialIn") {
        override fun transform(x: Float) = if (x == 0f) 0f else 2f.pow(10f * x - 10f)
    },
    EXPONENTIAL_OUT("ExponentialOut") {
        override fun transform(x: Float) = if (x == 1f) 1f else 1f - 2f.pow(-10f * x)
    },
    BACK_OUT("BackOut") {
        override fun transform(x: Float): Float {
            val c = 1.70158f
            val u = x - 1f
            return 1f + (c + 1f) * u * u * u + c * u * u
        }
    },
    BOUNCE_OUT("BounceOut") {
        override fun transform(x: Float): Float {
            val n = 7.5625f
            val d = 2.75f
            return when {
                x < 1f / d -> n * x * x
                x < 2f / d -> (x - 1.5f / d).let { n * it * it + 0.75f }
                x < 2.5f / d -> (x - 2.25f / d).let { n * it * it + 0.9375f }
                else -> (x - 2.625f / d).let { n * it * it + 0.984375f }
            }
        }
    },
    NONE("None") {
        override fun transform(x: Float) = 1f

        override fun getFactor(startTime: Long, currentTime: Long, time: Float) = 1f
    };

    abstract fun transform(x: Float): Float

    open fun getFactor(startTime: Long, currentTime: Long, time: Float): Float {
        val delta = currentTime - startTime
        val factor = (delta / time.toDouble()).toFloat().coerceIn(0F, 1F)
        return transform(factor)
    }

}
