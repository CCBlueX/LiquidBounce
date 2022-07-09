/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.sin
import kotlin.math.pow

/*
 * https://easings.net/
 * https://github.com/jesusgollonet/processing-penner-easing/tree/master/src
 */

/**
 * In-out-easing function https://github.com/jesusgollonet/processing-penner-easing
 *
 * @param  current
 * Current iteration
 * @param  total
 * Total iterations
 * @return         Eased value
 */
fun easeOut(current: Float, total: Float): Float
{
	var fixedCurrent = current
	return ((fixedCurrent / total - 1).also { fixedCurrent = it }) * fixedCurrent * fixedCurrent + 1
}

/**
 * Source: https://easings.net/#easeOutElastic
 *
 * @return A value larger than 0
 */
fun easeOutElastic(x: Float): Float
{
	val c4 = 2 * WMathHelper.PI / 3.0f

	return if (x == 0f) 0.0f else (if (x == 1f) 1.0 else 2.0.pow(-10.0 * x) * sin((x * 10 - 0.75f) * c4) + 1).toFloat()
}

fun easeOutCubic(easingValue: Float, originalValue: Float, speed: Int): Float = easingValue + ((originalValue - easingValue) / 2.0F.pow(10 - speed.coerceAtMost(9))) * RenderUtils.frameTime

