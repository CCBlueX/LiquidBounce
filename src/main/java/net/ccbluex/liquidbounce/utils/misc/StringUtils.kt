/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc

import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.util.*

@SideOnly(Side.CLIENT)
object StringUtils {
    fun toCompleteString(args: Array<String>, start: Int) =
        if (args.size <= start) ""
        else args.drop(start).joinToString(separator = " ")

    // TODO: Remove this unused method?
    fun replace(string: String, searchChars: String, replaceChars: String = ""): String {
        if (string.isEmpty() || searchChars.isEmpty() || searchChars == replaceChars) return string

        val stringLength = string.length
        val searchCharsLength = searchChars.length
        val stringBuilder = StringBuilder(string)

        for (i in 0 until stringLength) {
            val start = stringBuilder.indexOf(searchChars, i)

            if (start == -1) {
                return if (i == 0) string
                else stringBuilder.toString()
            }

            stringBuilder.replace(start, start + searchCharsLength, replaceChars)
        }

        return stringBuilder.toString()
    }
}
