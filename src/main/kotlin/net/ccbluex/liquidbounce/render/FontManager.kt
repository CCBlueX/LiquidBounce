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
package net.ccbluex.liquidbounce.render

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.AsyncLazy
import net.ccbluex.liquidbounce.render.engine.font.FontGlyphPageManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.util.Util
import net.minecraft.util.Util.OperatingSystem.*
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream

object FontManager {

    private val STYLES = intArrayOf(
        Font.BOLD,
        Font.BOLD,
        Font.ITALIC,
        Font.BOLD or Font.ITALIC
    )

    /**
     * As fallback, we can use a common font that is available on all systems.
     */
    private val COMMON_FONT by AsyncLazy {
        runCatching {
            when (Util.getOperatingSystem()) {
                WINDOWS -> systemFont("Segoe UI")
                OSX -> systemFont("Helvetica")
                LINUX -> systemFont("DejaVu Sans")
                else -> systemFont("Arial")
            }
        }.onFailure { throwable ->
            logger.error("Failed to load common font.", throwable)
        }.getOrNull() ?: systemFont("Arial")
    }

    /**
     * Default font for displaying CJK (Chinese, Japanese, Korean) characters.
     */
    private val CJK_FONT by AsyncLazy {
        runCatching {
            when (Util.getOperatingSystem()) {
                WINDOWS -> systemFont("Microsoft YaHei")
                OSX -> systemFont("PingFang SC")
                LINUX -> systemFont("Noto Sans CJK")
                else -> null // No default CJK font available
            }
        }.onFailure { throwable ->
            logger.error("Failed to load CJK font.", throwable)
        }.getOrNull()
    }

    /**
     * All font faces that are known to the font manager.
     */
    internal val fontFaces = HashMap<String, FontFace>(8).apply { put(COMMON_FONT.name, COMMON_FONT) }

    private fun addFontFace(fontFace: FontFace) {
        fontFaces[fontFace.name] = fontFace
    }

    /**
     * The active font renderer that all text rendering will be based on.
     *
     * TODO: Replaces this with Module-based Font Selection
     */
    val FONT_RENDERER
        get() = (fontFace("Inter Regular") ?: COMMON_FONT).renderer

    /**
     * Since our font renderer does not support dynamic font size changes,
     * we will use 43 as the default font size.
     */
    const val DEFAULT_FONT_SIZE: Float = 43f

    /**
     * The glyph manager that is responsible for managing the glyph pages.
     */
    var glyphManager: FontGlyphPageManager
        field: FontGlyphPageManager? = null
        private set
        get() = requireNotNull(field) { "Glyph manager was not initialized yet!" }

    /**
     * Returns the font by the given name.
     */
    internal fun fontFace(name: String) = fontFaces[name]

    internal fun createGlyphManager() {
        glyphManager = FontGlyphPageManager(
            baseFonts = fontFaces.values,
            additionalFonts = setOfNotNull(CJK_FONT)
        )
    }

    internal suspend fun queueFontFromFile(file: File) {
        try {
            if (!file.exists()) {
                logger.warn("Font file ${file.absolutePath} does not exist.")
                return
            }

            if (file.extension != "ttf") {
                logger.warn("Font file ${file.absolutePath} is not a TrueType font.")
                return
            }

            if (fontFaces.values.any { it.file == file }) {
                logger.warn("Font file ${file.absolutePath} is already loaded.")
                return
            }

            val font = Font
                .createFont(Font.TRUETYPE_FONT, file)
                .deriveFont(DEFAULT_FONT_SIZE)

            // Name will consist of the font name and family. This makes it possible
            // to select the different styles of the font.
            val fontFace = FontFace(font.name, DEFAULT_FONT_SIZE, file)
            // In this case, we have only one style available, which is the plain style.
            fontFace.fillStyle(font, 0)
            addFontFace(fontFace)
        } catch (e: Exception) {
            logger.warn("Failed to load font from file ${file.absolutePath}", e)
        }
    }

    internal suspend fun queueFontFromStream(stream: InputStream) {
        val font = Font.createFont(Font.TRUETYPE_FONT, stream)
            .deriveFont(DEFAULT_FONT_SIZE)
        val fontFace = FontFace(font.name, DEFAULT_FONT_SIZE, null)
        fontFace.fillStyle(font, 0)
        addFontFace(fontFace)
    }

    private suspend fun systemFont(name: String): FontFace {
        val fontFace = FontFace(name, DEFAULT_FONT_SIZE)

        STYLES.forEachIndexed { index, style ->
            val font = Font(name, style, DEFAULT_FONT_SIZE.toInt())
                .deriveFont(DEFAULT_FONT_SIZE)
            fontFace.fillStyle(font, index)
        }

        return fontFace
    }

    data class FontFace(
        val name: String,
        val size: Float,
        /**
         * The file of the font. If the font is a system font, this will be null.
         */
        val file: File? = null,
        /**
         * Style of the font. If an element is null, fall back to `[0]`
         *
         * [Font.PLAIN] -> 0 (Must not be null)
         *
         * [Font.BOLD] -> 1 (Can be null)
         *
         * [Font.ITALIC] -> 2 (Can be null)
         *
         * [Font.BOLD] | [Font.ITALIC] -> 3 (Can be null)
         */
        val styles: Array<FontId?> = arrayOfNulls(4)
    ) {

        // We only access it on the main thread so don't do synchronized
        val renderer: FontRenderer by lazy(LazyThreadSafetyMode.NONE) {
            FontRenderer(this, glyphManager!!)
        }

        /**
         * Fills the font style at the given index.
         */
        suspend fun fillStyle(font: Font, index: Int) = withContext(Dispatchers.Default) {
            val metrics = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics().apply {
                setFont(font)
            }.fontMetrics

            styles[index] = FontId(index, font, metrics.height.toFloat(), metrics.ascent.toFloat())
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

        override fun hashCode(): Int {
            var result = size.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + (file?.absolutePath?.hashCode() ?: 0)
            result = 31 * result + styles.contentHashCode()
            return result
        }

    }

    @JvmRecord
    data class FontId(
        val style: Int,
        val awtFont: Font,
        val height: Float,
        val ascent: Float
    )

}
