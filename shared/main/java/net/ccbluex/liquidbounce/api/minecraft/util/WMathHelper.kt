/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

import net.ccbluex.liquidbounce.LiquidBounce.wrapper

object WMathHelper
{
    const val PI = 3.141592653F

    @Suppress("FunctionName")
    @JvmStatic
    fun wrapAngleTo180_float(angle: Float): Float
    {
        var value = angle % 360.0f

        if (value >= 180.0f) value -= 360.0f
        if (value < -180.0f) value += 360.0f

        return value
    }

    @JvmStatic
    fun sin(radians: Float): Float = wrapper.functions.sin(radians)

    @JvmStatic
    fun cos(radians: Float): Float = wrapper.functions.cos(radians)

    @JvmStatic
    fun toRadians(degrees: Float): Float = degrees * 0.017453292F /* 1 / 180 * PI = 0.017453292... */

    @JvmStatic
    fun toDegrees(radians: Float): Float = radians * 57.295779513F /* 1 * 180 / PI = 57.295779513... */
}
