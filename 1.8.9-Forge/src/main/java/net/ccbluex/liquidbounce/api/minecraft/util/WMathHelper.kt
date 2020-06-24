/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

object WMathHelper {

    @Suppress("FunctionName")
    @JvmStatic
    fun wrapAngleTo180_float(angle: Float): Float {
        var value = angle % 360.0f

        if (value >= 180.0f) {
            value -= 360.0f
        }

        if (value < -180.0f) {
            value += 360.0f
        }

        return value
    }


}