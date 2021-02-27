/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc

import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.jetbrains.annotations.Contract
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.floor

@SideOnly(Side.CLIENT)
object StringUtils
{
	private val patternControlCode = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]")

	@JvmStatic
	fun toCompleteString(args: Array<String>, start: Int): String = if (args.size <= start) "" else args.copyOfRange(start, args.size).joinToString(separator = " ")

	@JvmStatic
	fun replace(string: String?, searchChars: String?, replaceChars: String?): String?
	{
		var fixedReplaceChars = replaceChars

		if (string.isNullOrEmpty() || searchChars.isNullOrEmpty() || searchChars == fixedReplaceChars) return string
		if (fixedReplaceChars == null) fixedReplaceChars = ""

		val stringLength = string.length
		val searchCharsLength = searchChars.length
		val stringBuilder = StringBuilder(string)

		repeat(stringLength) {
			val start = stringBuilder.indexOf(searchChars, it)

			if (start == -1) return@replace if (it == 0) string else "$stringBuilder"

			stringBuilder.replace(start, start + searchCharsLength, fixedReplaceChars)
		}

		return "$stringBuilder"
	}

	@JvmStatic
	fun getHorizontalFacing(yaw: Float): String
	{
		val facings = arrayOf("S", "SE", "E", "NE", "N", "NW", "W", "SW")

		return facings[abs(floor(yaw * facings.size / 360 + 0.5)).toInt() and facings.size - 1]
	}

	@JvmStatic
	fun getHorizontalFacingAdv(yaw: Float): String
	{
		val facings = arrayOf("S", "SSE", "SE", "SEE", "E", "NEE", "NE", "NNE", "N", "NNW", "NW", "NWW", "W", "SWW", "SW", "SSW")

		return facings[abs(floor(yaw * facings.size / 360 + 0.5)).toInt() and facings.size - 1]
	}

	@JvmStatic
	fun getHorizontalFacingTowards(yaw: Float): String
	{
		val facings = arrayOf("+Z", "+X +Z", "+X", "+X -Z", "-Z", "-X -Z", "-Z", "-X +Z")

		return facings[abs(floor(yaw * facings.size / 360 + 0.5)).toInt() and facings.size - 1]
	}

	@Contract("null -> null; !null ->!null")
	@JvmStatic
	fun stripControlCodes(text: String): String = patternControlCode.matcher(text).replaceAll("")
}
