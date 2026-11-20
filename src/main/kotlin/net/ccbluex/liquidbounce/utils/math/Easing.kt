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
import kotlin.math.abs
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

/**
 * CSS-style cubic bezier from (0, 0) over ([x1], [y1]) and ([x2], [y2]) to (1, 1), solved for [x].
 */
fun cubicBezier(x1: Float, y1: Float, x2: Float, y2: Float, x: Float): Float {
    fun curve(t: Float, a: Float, b: Float) = ((1 - 3 * b + 3 * a) * t + (3 * b - 6 * a)) * t * t + 3 * a * t
    fun slope(t: Float, a: Float, b: Float) = 3 * (1 - 3 * b + 3 * a) * t * t + 2 * (3 * b - 6 * a) * t + 3 * a

    var t = x
    repeat(8) {
        val error = curve(t, x1, x2) - x
        if (abs(error) < 1e-5f) {
            return curve(t, y1, y2)
        }
        val d = slope(t, x1, x2)
        if (abs(d) < 1e-6f) {
            return@repeat
        }
        t -= error / d
    }

    // Newton did not settle, bisect
    var low = 0f
    var high = 1f
    t = x
    repeat(32) {
        val value = curve(t, x1, x2)
        if (abs(value - x) < 1e-5f) {
            return curve(t, y1, y2)
        }
        if (value < x) low = t else high = t
        t = (low + high) / 2
    }
    return curve(t, y1, y2)
}
