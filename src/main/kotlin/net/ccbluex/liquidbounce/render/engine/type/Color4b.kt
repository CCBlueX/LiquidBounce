/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
@file:Suppress("TooManyFunctions")
package net.ccbluex.liquidbounce.render.engine.type

import org.lwjgl.opengl.GL20
import java.awt.Color

@JvmRecord
data class Color4b @JvmOverloads constructor(val r: Int, val g: Int, val b: Int, val a: Int = 255) {

    companion object {

        @JvmField
        val LIQUID_BOUNCE = Color4b(0x00, 0x80, 0xFF, 0xFF)
        @JvmField
        val WHITE = Color4b(255, 255, 255, 255)
        @JvmField
        val BLACK = Color4b(0, 0, 0, 255)
        @JvmField
        val RED = Color4b(255, 0, 0, 255)
        @JvmField
        val GREEN = Color4b(0, 255, 0, 255)
        @JvmField
        val BLUE = Color4b(0, 0, 255, 255)
        @JvmField
        val CYAN = Color4b(0, 255, 255, 255)
        @JvmField
        val MAGENTA = Color4b(255, 0, 255, 255)
        @JvmField
        val YELLOW = Color4b(255, 255, 0, 255)
        @JvmField
        val ORANGE = Color4b(255, 165, 0, 255)
        @JvmField
        val PURPLE = Color4b(128, 0, 128, 255)
        @JvmField
        val PINK = Color4b(255, 192, 203, 255)
        @JvmField
        val GRAY = Color4b(128, 128, 128, 255)
        @JvmField
        val LIGHT_GRAY = Color4b(192, 192, 192, 255)
        @JvmField
        val DARK_GRAY = Color4b(64, 64, 64, 255)
        @JvmField
        val TRANSPARENT = Color4b(0, 0, 0, 0)

        /**
         * Create a color from a hex string.
         *
         * @param hex The hex string. Can be in the format of "#RRGGBB" or "#AARRGGBB". (Prefix '#' is optional)
         * @return The color.
         * @throws IllegalArgumentException If the hex string is invalid.
         */
        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun fromHex(hex: String): Color4b {
            val cleanHex = hex.removePrefix("#")
            val hasAlpha = cleanHex.length == 8

            require(cleanHex.length == 6 || hasAlpha)

            return if (hasAlpha) {
                val rgba = cleanHex.toLong(16)
                Color4b(rgba.toInt(), hasAlpha = true)
            } else {
                val rgb = cleanHex.toInt(16)
                Color4b(
                    (rgb shr 16) and 0xFF,
                    (rgb shr 8) and 0xFF,
                    rgb and 0xFF,
                    255
                )
            }
        }
        fun rgbToHsl(color: Color4b): FloatArray {
            val r = color.r / 255f
            val g = color.g / 255f
            val b = color.b / 255f

            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            var h: Float
            val s: Float
            val l = (max + min) / 2f

            if (max == min) {
                h = 0f
                s = 0f
            } else {
                val d = max - min
                s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)

                h = when (max) {
                    r -> (g - b) / d + (if (g < b) 6f else 0f)
                    g -> (b - r) / d + 2f
                    else -> (r - g) / d + 4f
                }
                h /= 6f
            }

            return floatArrayOf(h, s, l)
        }

        fun hslToRgb(h: Float, s: Float, l: Float, alpha: Int = 255): Color4b {

            val normalizedH = ((h % 1f) + 1f) % 1f

            val r: Float
            val g: Float
            val b: Float

            if (s == 0f) {
                r = l
                g = l
                b = l
            } else {
                val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
                val p = 2 * l - q

                r = hueToRgb(p, q, normalizedH + 1f/3f)
                g = hueToRgb(p, q, normalizedH)
                b = hueToRgb(p, q, normalizedH - 1f/3f)
            }

            return Color4b((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), alpha)
        }

        fun hueToRgb(p: Float, q: Float, t: Float): Float {
            var t = t
            if (t < 0f) t += 1f
            if (t > 1f) t -= 1f
            return when {
                t < 1f/6f -> p + (q - p) * 6f * t
                t < 1f/2f -> q
                t < 2f/3f -> p + (q - p) * (2f/3f - t) * 6f
                else -> p
            }
        }
        fun lerp(from: Color4b, to: Color4b, t: Float): Color4b {
            return Color4b(
                (from.r + (to.r - from.r) * t).toInt(),
                (from.g + (to.g - from.g) * t).toInt(),
                (from.b + (to.b - from.b) * t).toInt(),
                (from.a + (to.a - from.a) * t).toInt()
            )
        }

    }

    constructor(color: Color) : this(color.red, color.green, color.blue, color.alpha)
    @JvmOverloads
    constructor(hex: Int, hasAlpha: Boolean = false) : this(
        r = (hex shr 16) and 0xFF,
        g = (hex shr 8) and 0xFF,
        b = hex and 0xFF,
        a = if (hasAlpha) (hex shr 24) and 0xFF else 255
    )

    fun blend(other: Color4b, factor: Float): Color4b {
        val invFactor = 1f - factor
        return Color4b(
            (r * invFactor + other.r * factor).toInt(),
            (g * invFactor + other.g * factor).toInt(),
            (b * invFactor + other.b * factor).toInt(),
            (a * invFactor + other.a * factor).toInt()
        )
    }
    fun with(
        r: Int = this.r,
        g: Int = this.g,
        b: Int = this.b,
        a: Int = this.a
    ): Color4b {
        return Color4b(r, g, b, a)
    }

    fun alpha(alpha: Int) = with(a = alpha)

    fun toARGB() = (a shl 24) or (r shl 16) or (g shl 8) or b

    fun toABGR() = (a shl 24) or (b shl 16) or (g shl 8) or r

    fun withAlpha(alpha: Int): Color4b {
        return Color4b(r, g, b, alpha.coerceIn(0, 255))
    }

    fun fade(fade: Float): Color4b {
        return if (fade >= 1.0f) {
            this
        } else {
            with(a = (a * fade).toInt())
        }
    }

    fun darker() = Color4b(darkerChannel(r), darkerChannel(g), darkerChannel(b), a)

    private fun darkerChannel(value: Int) = (value * 0.7).toInt().coerceAtLeast(0)

    fun putToUniform(pointer: Int) {
        GL20.glUniform4f(pointer, r / 255f, g / 255f, b / 255f, a / 255f)
    }

    /**
     * Interpolates this color with another color using the given percentage.
     *
     * @param other The color to interpolate to
     * @param percentage The percentage of interpolation (0.0 to 1.0)
     * @return The interpolated color
     */
    fun interpolateTo(other: Color4b, percentage: Double): Color4b =
        interpolateTo(other, percentage, percentage, percentage, percentage)

    /**
     * Interpolates this color with another color using separate factors for each component.
     *
     * @param other The color to interpolate to
     * @param tR The factor to interpolate the red value (0.0 to 1.0)
     * @param tG The factor to interpolate the green value (0.0 to 1.0)
     * @param tB The factor to interpolate the blue value (0.0 to 1.0)
     * @param tA The factor to interpolate the alpha value (0.0 to 1.0)
     * @return The interpolated color
     */
    fun interpolateTo(
        other: Color4b,
        tR: Double,
        tG: Double,
        tB: Double,
        tA: Double
    ): Color4b = Color4b(
        (r + (other.r - r) * tR).toInt().coerceIn(0, 255),
        (g + (other.g - g) * tG).toInt().coerceIn(0, 255),
        (b + (other.b - b) * tB).toInt().coerceIn(0, 255),
        (a + (other.a - a) * tA).toInt().coerceIn(0, 255)
    )

    /**
     * Converts this Color4b to a Java AWT Color
     *
     * @return The Color object representation
     */
    fun toAwtColor(): Color = Color(r, g, b, a)
}
