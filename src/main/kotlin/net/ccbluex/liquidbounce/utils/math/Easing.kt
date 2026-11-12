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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Functions from https://easings.net.
 */
@Suppress("unused")
enum class Easing(override val tag: String) : Tagged {

    LINEAR("Linear") {
        override fun transform(x: Float) = x
    },
    SINE_IN("SineIn") {
        override fun transform(x: Float) = 1 - cos(x * PI.toFloat() / 2)
    },
    SINE_OUT("SineOut") {
        override fun transform(x: Float) = sin(x * PI.toFloat() / 2)
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
    CUBIC_IN("CubicIn") {
        override fun transform(x: Float) = x * x * x
    },
    CUBIC_OUT("CubicOut") {
        override fun transform(x: Float) = 1 - (1 - x).pow(3)
    },
    QUART_IN("QuartIn") {
        override fun transform(x: Float) = x.pow(4)
    },
    QUART_OUT("QuartOut") {
        override fun transform(x: Float) = 1 - (1 - x).pow(4)
    },
    QUINT_IN("QuintIn") {
        override fun transform(x: Float) = x.pow(5)
    },
    QUINT_OUT("QuintOut") {
        override fun transform(x: Float) = 1 - (1 - x).pow(5)
    },
    EXPONENTIAL_IN("ExponentialIn") {
        override fun transform(x: Float) = if (x == 0f) 0f else 2f.pow(10f * x - 10f)
    },
    EXPONENTIAL_OUT("ExponentialOut") {
        override fun transform(x: Float) = if (x == 1f) 1f else 1f - 2f.pow(-10f * x)
    },
    CIRCULAR_IN("CircularIn") {
        override fun transform(x: Float) = 1 - sqrt(1 - x * x)
    },
    CIRCULAR_OUT("CircularOut") {
        override fun transform(x: Float) = sqrt(1 - (x - 1) * (x - 1))
    },
    BACK_IN("BackIn") {
        override fun transform(x: Float) = (BACK_OVERSHOOT + 1) * x * x * x - BACK_OVERSHOOT * x * x
    },
    BACK_OUT("BackOut") {
        override fun transform(x: Float) = 1 - BACK_IN.transform(1 - x)
    },
    ELASTIC_IN("ElasticIn") {
        override fun transform(x: Float) = when (x) {
            0f, 1f -> x
            else -> -2f.pow(10f * x - 10f) * sin((10f * x - 10.75f) * ELASTIC_PERIOD)
        }
    },
    ELASTIC_OUT("ElasticOut") {
        override fun transform(x: Float) = 1 - ELASTIC_IN.transform(1 - x)
    },
    BOUNCE_IN("BounceIn") {
        override fun transform(x: Float) = 1 - BOUNCE_OUT.transform(1 - x)
    },
    BOUNCE_OUT("BounceOut") {
        override fun transform(x: Float) = when {
            x < 1 / 2.75f -> 7.5625f * x * x
            x < 2 / 2.75f -> 7.5625f * (x - 1.5f / 2.75f).pow(2) + 0.75f
            x < 2.5f / 2.75f -> 7.5625f * (x - 2.25f / 2.75f).pow(2) + 0.9375f
            else -> 7.5625f * (x - 2.625f / 2.75f).pow(2) + 0.984375f
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

private const val BACK_OVERSHOOT = 1.70158f
private const val ELASTIC_PERIOD = (2 * PI / 3).toFloat()

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
