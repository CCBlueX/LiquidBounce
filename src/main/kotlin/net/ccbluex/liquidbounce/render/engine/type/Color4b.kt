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
data class Color4b(val r: Int, val g: Int, val b: Int, val a: Int = 255) {

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

        @Throws(IllegalArgumentException::class)
        fun fromHex(hex: String): Color4b {
            val cleanHex = hex.removePrefix("#")
            val hasAlpha = cleanHex.length == 8

            require(cleanHex.length == 6 || hasAlpha)

            return if (hasAlpha) {
                val rgba = cleanHex.toLong(16)
                Color4b(
                    (rgba shr 24).toInt() and 0xFF,
                    (rgba shr 16).toInt() and 0xFF,
                    (rgba shr 8).toInt() and 0xFF,
                    rgba.toInt() and 0xFF
                )
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

    }

    constructor(color: Color) : this(color.red, color.green, color.blue, color.alpha)
    constructor(hex: Int, hasAlpha: Boolean = false) : this(Color(hex, hasAlpha))

    fun with(
        r: Int = this.r,
        g: Int = this.g,
        b: Int = this.b,
        a: Int = this.a
    ): Color4b {
        return Color4b(r, g, b, a)
    }

    fun alpha(alpha: Int) = Color4b(this.r, this.g, this.b, alpha)

    fun toARGB() = (a shl 24) or (r shl 16) or (g shl 8) or b

    fun toABGR() = (a shl 24) or (b shl 16) or (g shl 8) or r

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
        ((r + (other.r - r) * tR)).toInt().coerceIn(0, 255),
        ((g + (other.g - g) * tG)).toInt().coerceIn(0, 255),
        ((b + (other.b - b) * tB)).toInt().coerceIn(0, 255),
        ((a + (other.a - a) * tA)).toInt().coerceIn(0, 255)
    )

    /**
     * Converts this Color4b to a Java AWT Color
     *
     * @return The Color object representation
     */
    fun toAwtColor(): Color = Color(r, g, b, a)
}
