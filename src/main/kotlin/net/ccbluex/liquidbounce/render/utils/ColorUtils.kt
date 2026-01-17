/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.render.utils

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import java.awt.Color

object ColorUtils {
    @JvmField
    val hexColors = IntArray(16)

    init {
        repeat(16) { i ->
            val baseColor = (i shr 3 and 1) * 85

            val red = (i shr 2 and 1) * 170 + baseColor + if (i == 6) 85 else 0
            val green = (i shr 1 and 1) * 170 + baseColor
            val blue = (i and 1) * 170 + baseColor

            hexColors[i] = red and 255 shl 16 or (green and 255 shl 8) or (blue and 255)
        }
    }
}

@JvmOverloads
fun rainbow(alpha: Float = 1f): Color4b {
    return Color4b.ofHSB(
        hue = (System.nanoTime().toDouble() / 10_000_000_000.0).toFloat() % 1.0F,
        saturation = 1F,
        brightness = 1F,
        alpha = alpha,
    )
}

fun shiftHue(color4b: Color4b, shift: Int): Color4b {
    val hsb = Color.RGBtoHSB(color4b.r, color4b.g, color4b.b, null)
    return Color4b.ofHSB(
        hue = (hsb[0] + shift.toFloat() / 360) % 1F,
        saturation = hsb[1],
        brightness = hsb[2],
        alpha = color4b.a / 255F,
    )
}

fun interpolateHue(primaryColor: Color4b, otherColor: Color4b, percentageOther: Float): Color4b {
    val hsb1 = FloatArray(3)
    val hsb2 = FloatArray(3)
    Color.RGBtoHSB(primaryColor.r,primaryColor.g, primaryColor.b, hsb1)
    Color.RGBtoHSB(otherColor.r, otherColor.g, otherColor.b, hsb2)

    val h = hsb1[0] + (hsb2[0] - hsb1[0]) * percentageOther
    val s = hsb1[1] + (hsb2[1] - hsb1[1]) * percentageOther
    val v = hsb1[2] + (hsb2[2] - hsb1[2]) * percentageOther
    val alpha = primaryColor.a + (otherColor.a - primaryColor.a) * percentageOther

    val rgb = Color.HSBtoRGB(h, s, v)
    return Color4b(
        (rgb shr 16) and 0xFF,
        (rgb shr 8) and 0xFF,
        rgb and 0xFF,
        alpha.toInt()
    )
}
