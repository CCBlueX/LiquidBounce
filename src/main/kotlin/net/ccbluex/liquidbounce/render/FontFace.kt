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
import net.ccbluex.liquidbounce.render.engine.font.FontId
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.engine.font.FontStyle
import net.ccbluex.liquidbounce.render.engine.font.GlyphPage.Companion.fontRasterizationLock
import net.ccbluex.liquidbounce.render.engine.font.TextMetrics
import okio.ByteString
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.time.Instant

class FontFace(
    val name: String,
    val size: Float,
    val source: Source = Source.Unspecified(Instant.now()),
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

    /** How each style in [styles] lays out, filled by the same call. */
    private val metrics: Array<TextMetrics?> = arrayOfNulls(4)

    private var cachedRenderer: FontRenderer? = null

    // We only access it on the main thread so don't do synchronized
    val renderer: FontRenderer
        get() {
            val glyphManager = FontManager.glyphManager
            return cachedRenderer?.takeIf { it.glyphManager === glyphManager }
                ?: FontRenderer(this, glyphManager).also { cachedRenderer = it }
        }

    val plainStyle: FontId
        get() = requireNotNull(styles[0]) {
            "FontFace ${name}_$size has no plain style!"
        }

    val plainMetrics: TextMetrics
        get() = requireNotNull(metrics[0]) {
            "FontFace ${name}_$size has no plain metrics!"
        }

    val filledStyles: List<FontId> get() = styles.filterNotNull()

    fun style(style: Int): FontId? = styles.getOrNull(style)

    internal suspend fun fillDerivedStyles(baseFont: Font) {
        for (style in 0..3) {
            val font = if (style == Font.PLAIN) baseFont else baseFont.deriveFont(style, size)
            fillStyle(font, style)
        }
    }

    /**
     * Fills the font style at the given index.
     */
    suspend fun fillStyle(font: Font, style: @FontStyle Int) {
        if (style !in 0..3) {
            error("Illegal Style $style, should be PLAIN/BOLD/ITALIC/BOLD+ITALIC")
        }

        withContext(Dispatchers.Default) {
            val (fontId, textMetrics) = synchronized(fontRasterizationLock) {
                val graphics = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
                try {
                    graphics.font = font
                    val fontMetrics = graphics.fontMetrics
                    val lineMetrics = font.getLineMetrics("Ag", graphics.fontRenderContext)

                    FontId(font) to TextMetrics(
                        height = fontMetrics.height.toFloat(),
                        ascent = fontMetrics.ascent.toFloat(),
                        underlineOffset = lineMetrics.underlineOffset,
                        underlineThickness = lineMetrics.underlineThickness,
                        strikethroughOffset = lineMetrics.strikethroughOffset,
                        strikethroughThickness = lineMetrics.strikethroughThickness,
                    )
                } finally {
                    graphics.dispose()
                }
            }

            styles[style] = fontId
            metrics[style] = textMetrics
        }
    }

    override fun toString(): String {
        return "FontFace(name='$name', size=$size, source=$source)"
    }

    /**
     * Source type and identity.
     */
    sealed interface Source {
        /**
         * Only for default constructor.
         */
        @ConsistentCopyVisibility
        data class Unspecified internal constructor(val instant: Instant) : Source

        /**
         * Loaded via AWT system font lookup with name.
         */
        data class System(val name: String) : Source

        /**
         * Loaded from [path].
         */
        data class LocalFile(val path: File) : Source

        /**
         * Loaded from in-memory content with hash [sha256].
         */
        data class InMemory(val sha256: ByteString) : Source
    }

}
