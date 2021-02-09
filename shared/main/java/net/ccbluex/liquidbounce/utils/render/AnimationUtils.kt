/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.sin

object AnimationUtils
{
	/**
	 * In-out-easing function https://github.com/jesusgollonet/processing-penner-easing
	 *
	 * @param  current
	 * Current iteration
	 * @param  total
	 * Total iterations
	 * @return         Eased value
	 */
	@JvmStatic
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
	@JvmStatic
	fun easeOutElastic(x: Float): Float
	{
		val c4 = 2 * WMathHelper.PI / 3.0f

		return if (x == 0f) 0.0f else (if (x == 1f) 1.0 else StrictMath.pow(2.0, -10.0 * x) * sin((x * 10 - 0.75f) * c4) + 1).toFloat()
	}
}
