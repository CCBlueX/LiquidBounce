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

package net.ccbluex.liquidbounce.render.engine.font

@JvmRecord
data class GlyphIdentifier(val codepoint: Char, val style: Int) {
    constructor(fontGlyph: FontGlyph) : this(fontGlyph.codepoint, fontGlyph.font.style)
    constructor(longValue: Long) : this(
        codepoint = unpackCodepoint(longValue),
        style = unpackStyle(longValue),
    )

    fun asLong(): Long = asLong(codepoint, style)

    companion object {
        @JvmStatic
        fun asLong(codepoint: Char, style: Int) = (style.toLong() shl 32) or codepoint.code.toLong()

        @JvmStatic
        fun asLong(fontGlyph: FontGlyph) = asLong(fontGlyph.codepoint, fontGlyph.font.style)

        @JvmStatic
        fun unpackCodepoint(longValue: Long): Char = (longValue and Char.MAX_VALUE.code.toLong()).toInt().toChar()

        @JvmStatic
        fun unpackStyle(longValue: Long): Int = (longValue shr 32).toInt()
    }
}
