/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.floor

object StringUtils
{
    @JvmStatic
    val URL_PATTERN: Pattern = Pattern.compile("((?:[a-z\\d]{2,}://)?(?:(?:\\d{1,3}\\.){3}\\d{1,3}|[-\\w_.]+\\.[a-z]{2,}?)(?::\\d{1,5})?.*?(?=[!\"\u00A7 \n]|$))", Pattern.CASE_INSENSITIVE)

    @JvmStatic
    private val patternControlCode = Pattern.compile("(?i)\\u00A7[\\dA-FK-OR]")

    @JvmStatic
    val DECIMALFORMAT_1 = DecimalFormat("##0.0", DecimalFormatSymbols(Locale.ENGLISH))

    @JvmStatic
    val DECIMALFORMAT_2 = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))

    @JvmStatic
    val DECIMALFORMAT_4 = DecimalFormat("##0.0000", DecimalFormatSymbols(Locale.ENGLISH))

    @JvmStatic
    val DECIMALFORMAT_6 = DecimalFormat("##0.000000", DecimalFormatSymbols(Locale.ENGLISH))

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
        val facings = arrayOf("+Z", "+X +Z", "+X", "+X -Z", "-Z", "-X -Z", "-X", "-X +Z")

        return facings[abs(floor(yaw * facings.size / 360 + 0.5)).toInt() and facings.size - 1]
    }

    @JvmStatic
    fun stripControlCodes(text: String): String = patternControlCode.matcher(text).replaceAll("")
}
