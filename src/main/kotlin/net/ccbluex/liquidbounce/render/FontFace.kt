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

package net.ccbluex.liquidbounce.render

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.render.engine.FontId
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import kotlin.jvm.Volatile

class FontFace(
    val name: String,
    val size: Float,
    /**
     * The file of the font. If the font is a system font, this will be null.
     */
    val file: File? = null,
) {
    /**
     * Style of the font. If an element is null, fall back to `[0]`
     *
     * [java.awt.Font.PLAIN] -> 0 (Must not be null)
     *
     * [java.awt.Font.BOLD] -> 1 (Can be null)
     *
     * [java.awt.Font.ITALIC] -> 2 (Can be null)
     *
     * [java.awt.Font.BOLD] | [java.awt.Font.ITALIC] -> 3 (Can be null)
     */
    private val styles: Array<FontId?> = arrayOfNulls(4)

    @Volatile
    private var cachedHash: Int = 0

    // We only access it on the main thread so don't do synchronized
    val renderer: FontRenderer by lazy(LazyThreadSafetyMode.NONE) {
        FontRenderer(this, FontManager.glyphManager)
    }

    val plainStyle: FontId
        get() = requireNotNull(styles[0]) {
            "FontFace ${name}_$size has no plain style!"
        }

    val filledStyles: List<FontId> get() = styles.filterNotNull()

    fun style(style: Int): FontId? = styles.getOrNull(style)

    /**
     * Fills the font style at the given index.
     */
    suspend fun fillStyle(font: Font, style: Int) {
        if (style !in 0..3) {
            error("Illegal Style $style, should be PLAIN/BOLD/ITALIC/BOLD+ITALIC")
        }

        withContext(Dispatchers.Default) {
            val metrics = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics().apply {
                setFont(font)
            }.fontMetrics

            styles[style] = FontId(style, font, metrics.height.toFloat(), metrics.ascent.toFloat())
            cachedHash = 0
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FontFace) return false

        if (size != other.size) return false
        if (name != other.name) return false
        if (file != other.file) return false
        if (!styles.contentEquals(other.styles)) return false

        return true
    }

    /**
     * @see java.lang.String.hashCode
     */
    override fun hashCode(): Int {
        var h = cachedHash
        if (h == 0) {
            var result = size.hashCode() // size != 0
            result = 31 * result + name.hashCode()
            result = 31 * result + (file?.absolutePath?.hashCode() ?: 0)
            result = 31 * result + styles.contentHashCode()
            h = result
            cachedHash = h
        }
        return h
    }

}
