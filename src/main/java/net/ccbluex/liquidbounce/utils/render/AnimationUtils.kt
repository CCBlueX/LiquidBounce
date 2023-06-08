/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import kotlin.math.pow
import kotlin.math.sin

object AnimationUtils {
    /**
     * In-out-easing function
     * https://github.com/jesusgollonet/processing-penner-easing
     *
     * @param t Current iteration
     * @param d Total iterations
     * @return Eased value
     */
    fun easeOut(t: Float, d: Float) = (t / d - 1).pow(3) + 1

    /**
     * Source: https://easings.net/#easeOutElastic
     *
     * @return A value larger than 0
     */
    fun easeOutElastic(x: Float) =
        when (x) {
            0f, 1f -> x
            else -> 2f.pow(-10 * x) * sin((x * 10 - 0.75f) * (2 * Math.PI / 3f).toFloat()) + 1
        }
}
