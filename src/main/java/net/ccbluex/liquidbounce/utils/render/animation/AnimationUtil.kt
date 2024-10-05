package net.ccbluex.liquidbounce.utils.render.animation

import net.minecraft.client.Minecraft
import kotlin.math.*

object AnimationUtil {
    val debugFPS: Float
        get() = max(Minecraft.getDebugFPS().toFloat(), 60f)

    fun base(current: Double, target: Double, speed: Double): Double {
        return ((current + (target - current) * (speed / (debugFPS / 60.0))) * 1000).toInt() / 1000.0
    }

    fun linear(startTime: Long, duration: Long, start: Double, end: Double): Double {
        return (end - start) * ((System.currentTimeMillis() - startTime) * 1.0 / duration) + start
    }

    fun easeInQuad(startTime: Long, duration: Long, start: Double, end: Double): Double {
        return (end - start) * ((System.currentTimeMillis() - startTime) * 1.0 / duration).pow(2.0) + start
    }

    fun easeOutQuad(startTime: Long, duration: Long, start: Double, end: Double): Double {
        val x = (System.currentTimeMillis() - startTime) * 1.0f / duration
        val y = -x * x + 2 * x
        return start + (end - start) * y
    }

    fun easeInOutQuad(startTime: Long, duration: Long, start: Double, end: Double): Double {
        var t = (System.currentTimeMillis() - startTime) * 1.0f / duration
        t *= 2f
        return if (t < 1) {
            (end - start) / 2 * t * t + start
        } else {
            t--
            -(end - start) / 2 * (t * (t - 2) - 1) + start
        }
    }

    fun easeInElastic(t: Double, b: Double, c: Double, d: Double): Double {
        var t = t
        var s = 1.70158
        var p = 0.0
        var a = c
        if (t == 0.0) return b
        t /= d
        if (t == 1.0) return b + c
        p = d * 0.3
        if (a < abs(c)) {
            a = c
            s = p / 4.0
        } else {
            s = p / (2 * Math.PI) * asin(c / a)
        }
        t--
        return -(a * 2.0.pow(10 * t) * sin((t * d - s) * (2 * Math.PI) / p)) + b
    }

    fun easeOutElastic(t: Double, b: Double, c: Double, d: Double): Double {
        var t = t
        var s = 1.70158
        var p = 0.0
        var a = c
        if (t == 0.0) return b
        t /= d
        if (t == 1.0) return b + c
        p = d * 0.3
        if (a < abs(c)) {
            a = c
            s = p / 4.0
        } else {
            s = p / (2 * Math.PI) * asin(c / a)
        }
        return a * 2.0.pow(-10 * t) * sin((t * d - s) * (2 * Math.PI) / p) + c + b
    }

    fun easeInOutElastic(t: Double, b: Double, c: Double, d: Double): Double {
        var t = t
        var s = 1.70158
        var p = 0.0
        var a = c
        if (t == 0.0) return b
        t /= d / 2
        if (t == 2.0) return b + c
        p = d * (0.3 * 1.5)
        if (a < abs(c)) {
            a = c
            s = p / 4.0
        } else {
            s = p / (2 * Math.PI) * asin(c / a)
        }
        return if (t < 1) {
            t--
            -0.5 * (a * 2.0.pow(10 * t) * sin((t * d - s) * (2 * Math.PI) / p)) + b
        } else {
            t--
            a * 2.0.pow(-10 * t) * sin((t * d - s) * (2 * Math.PI) / p) * 0.5 + c + b
        }
    }

    fun easeInBack(t: Double, b: Double, c: Double, d: Double): Double {
        var t = t
        val s = 1.70158
        t /= d
        return c * t * t * ((s + 1) * t - s) + b
    }

    fun easeOutBack(t: Double, b: Double, c: Double, d: Double): Double {
        var t = t
        val s = 1.70158
        t = t / d - 1
        return c * (t * t * ((s + 1) * t + s) + 1) + b
    }
}
