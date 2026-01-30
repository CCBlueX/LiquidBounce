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
@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.render.engine.type

import net.minecraft.network.chat.TextColor
import net.minecraft.util.ARGB
import org.joml.Vector4f
import java.awt.Color

@JvmRecord
data class Color4b(val argb: Int) {

    @JvmOverloads
    constructor(r: Int, g: Int, b: Int, a: Int = 255) : this(ARGB.color(a, r, g, b))

    constructor(color: Color) : this(color.rgb)

    @get:JvmName("a")
    val a: Int get() = ARGB.alpha(argb)

    @get:JvmName("r")
    val r: Int get() = ARGB.red(argb)

    @get:JvmName("g")
    val g: Int get() = ARGB.green(argb)

    @get:JvmName("b")
    val b: Int get() = ARGB.blue(argb)

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
        val TRANSPARENT = Color4b(0)

        @JvmField
        val DEFAULT_BG_COLOR = Color4b(Int.MIN_VALUE)

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
                Color4b(rgba.toInt())
            } else {
                val rgb = cleanHex.toInt(16)
                fullAlpha(rgb)
            }
        }

        /**
         * Create a color from HSB values.
         *
         * @param hue The hue value (0.0 to 1.0)
         * @param saturation The saturation value (0.0 to 1.0)
         * @param brightness The brightness value (0.0 to 1.0)
         * @param alpha The alpha value (0.0 to 1.0)
         * @return The color
         */
        @JvmStatic
        @JvmOverloads
        fun ofHSB(
            hue: Float,
            saturation: Float,
            brightness: Float,
            alpha: Float = 1f,
        ): Color4b {
            val rgb = Color.HSBtoRGB(hue, saturation, brightness)
            return Color4b(
                r = (rgb shr 16) and 0xFF,
                g = (rgb shr 8) and 0xFF,
                b = rgb and 0xFF,
                a = (alpha * 255).toInt(),
            )
        }

        /**
         * Creates a color with full alpha (255).
         */
        @JvmStatic
        fun fullAlpha(rgb: Int): Color4b = Color4b(rgb or 0xFF000000.toInt())
    }

    val isTransparent: Boolean
        get() = a <= 0

    @Suppress("NOTHING_TO_INLINE")
    inline fun with(
        r: Int = this.r,
        g: Int = this.g,
        b: Int = this.b,
        a: Int = this.a
    ): Color4b {
        return Color4b(r, g, b, a)
    }

    fun alpha(alpha: Int) = with(a = alpha)

    @Deprecated(
        message = "Replaced with Color4b.argb",
        replaceWith = ReplaceWith("this.argb"),
        level = DeprecationLevel.ERROR, // For script compatibility only
    )
    fun toARGB() = this.argb

    fun fade(fade: Float): Color4b {
        return if (fade >= 1.0f) {
            this
        } else {
            alpha((a * fade).toInt())
        }
    }

    fun darker() = Color4b(darkerChannel(r), darkerChannel(g), darkerChannel(b), a)

    private fun darkerChannel(value: Int) = (value * 0.7).toInt().coerceAtLeast(0)

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
     * Converts this [Color4b] to a Java AWT Color
     *
     * @return The Color object representation
     */
    fun toAwtColor(): Color = Color(r, g, b, a)

    fun toTextColor(): TextColor = TextColor.fromRgb(argb)

    /**
     * @return the ARGB value in hex string with [format].
     */
    @JvmOverloads
    fun toHexString(format: HexFormat = HexFormat.Default): String =
        argb.toHexString(format)

    @JvmOverloads
    fun toVector4f(dest: Vector4f = Vector4f()): Vector4f {
        return dest.set(r / 255f, g / 255f, b / 255f, a / 255f)
    }
}
