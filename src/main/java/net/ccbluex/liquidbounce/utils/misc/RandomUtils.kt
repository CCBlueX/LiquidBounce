/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc

import kotlin.random.Random

object RandomUtils
{
	@JvmStatic
	fun nextInt(startInclusive: Int, endExclusive: Int): Int = if (endExclusive - startInclusive <= 0) startInclusive else startInclusive + Random.nextInt(endExclusive - startInclusive)

	//	@JvmStatic
	//	fun nextDouble(startInclusive: Double, endInclusive: Double): Double = if (startInclusive == endInclusive || endInclusive - startInclusive <= 0.0) startInclusive else startInclusive + (endInclusive - startInclusive) * Math.random()

	@JvmStatic
	fun nextFloat(startInclusive: Float, endInclusive: Float): Float = if (startInclusive == endInclusive || endInclusive - startInclusive <= 0f) startInclusive else (startInclusive + (endInclusive - startInclusive) * Random.nextFloat())

	@JvmStatic
	fun randomNumber(length: Int): String = random(length, "123456789")

	@JvmStatic
	fun randomString(length: Int): String = random(length, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")

	@JvmStatic
	fun random(length: Int, chars: String): String = random(length, chars.toCharArray())

	@JvmStatic
	fun random(length: Int, chars: CharArray): String
	{
		val stringBuilder = StringBuilder()
		val charsSize = chars.size

		repeat(length) { stringBuilder.append(chars[Random.nextInt(charsSize)]) }

		return "$stringBuilder"
	}
}
