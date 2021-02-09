/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

import net.ccbluex.liquidbounce.LiquidBounce

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

	@Suppress("FunctionName")
	@JvmStatic
	fun clamp_float(num: Float, min: Float, max: Float): Float = if (num < min) min else if (num > max) max else num

	@Suppress("FunctionName")
	@JvmStatic
	fun clamp_double(num: Double, min: Double, max: Double): Double = if (num < min) min else if (num > max) max else num

	@JvmStatic
	fun sin(radians: Float): Float = LiquidBounce.wrapper.functions.sin(radians)

	@JvmStatic
	fun cos(radians: Float): Float = LiquidBounce.wrapper.functions.cos(radians)

	@JvmStatic
	fun toRadians(degrees: Float): Float = degrees * 0.017453292F /* 1 / 180 * PI = 0.017453292... */

	@JvmStatic
	fun toDegrees(radians: Float): Float = radians * 57.295779513F /* 1 * 180 / PI = 57.295779513... */
}
