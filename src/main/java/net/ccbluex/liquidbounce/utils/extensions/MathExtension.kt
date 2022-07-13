package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.util.MathHelper

const val PI = 3.1415927F

@Suppress("FunctionName")
val Float.wrapAngleTo180: Float
    get()
    {
        var value = this % 360.0f
        if (value >= 180.0f) value -= 360.0f
        if (value < -180.0f) value += 360.0f

        return value
    }

// BetterFps-compatible solution
val Float.sin: Float
    get() = MathHelper.sin(this)

// BetterFps-compatible solution
val Float.cos: Float
    get() = MathHelper.cos(this)

val Float.toRadians: Float
    get() = this * 0.017453292F /* 1 / 180 * PI = 0.017453292... */

val Float.toDegrees: Float
    get() = this * 57.29578F /* 1 * 180 / PI = 57.295779513... */
