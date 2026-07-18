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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.AsyncLazy
import net.ccbluex.liquidbounce.render.engine.font.FontGlyphPageManager
import net.ccbluex.liquidbounce.utils.client.clientLogger
import net.ccbluex.liquidbounce.utils.io.createFont
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.minecraft.util.Util
import net.minecraft.util.Util.OS.LINUX
import net.minecraft.util.Util.OS.OSX
import net.minecraft.util.Util.OS.WINDOWS
import java.awt.Font
import java.io.File
import java.io.InputStream

object FontManager {

    private val logger = clientLogger("FontManager")

    private val STYLES = intArrayOf(
        Font.PLAIN,
        Font.BOLD,
        Font.ITALIC,
        Font.BOLD or Font.ITALIC,
    )

    /**
     * As fallback, we can use a common font that is available on all systems.
     */
    private val COMMON_FONT by AsyncLazy {
        runCatching {
            when (Util.getPlatform()) {
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
            when (Util.getPlatform()) {
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
    internal val fontFaces = Object2ObjectOpenHashMap<String, FontFace>(8).apply {
        put(COMMON_FONT.name, COMMON_FONT)
    }

    private suspend fun addFontFace(fontFace: FontFace) = withContext(Dispatchers.Minecraft) {
        if (fontFaces.put(fontFace.name, fontFace) != null) {
            logger.warn("FontFace ${fontFace.name} already exists, previous one has been replaced")
        }
    }

    /**
     * The active font renderer that all text rendering will be based on.
     *
     * TODO: Replaces this with Module-based Font Selection
     */
    val FONT_RENDERER
        get() = defaultFontFace.renderer

    private val defaultFontFace
        get() = fontFace("Inter Regular") ?: COMMON_FONT

    /**
     * Since our font renderer does not support dynamic font size changes,
     * we will use 43 as the default font size.
     */
    const val DEFAULT_FONT_SIZE: Float = 43f

    private var _glyphManager: FontGlyphPageManager? = null
    /**
     * The glyph manager that is responsible for managing the glyph pages.
     */
    val glyphManager: FontGlyphPageManager
        get() = requireNotNull(_glyphManager) { "Glyph manager was not initialized yet!" }

    /**
     * Returns the font by the given name.
     */
    fun fontFace(name: String) = fontFaces[name]

    internal fun createGlyphManager() {
        _glyphManager?.close()
        _glyphManager = FontGlyphPageManager(
            registeredFaces = ObjectImmutableList(fontFaces.values),
            primaryFace = defaultFontFace,
            fallbackFonts = listOfNotNull(COMMON_FONT, CJK_FONT),
        )
    }

    internal fun closeGlyphManager() {
        _glyphManager?.close()
        _glyphManager = null
    }

    internal suspend fun queueFontFromFile(file: File) {
        try {
            if (!file.exists()) {
                logger.warn("Font file ${file.absolutePath} does not exist.")
                return
            }

            if (!file.extension.equals("ttf", ignoreCase = true)) {
                logger.warn("Font file ${file.absolutePath} is not a TrueType font.")
                return
            }

            if (fontFaces.values.any { it.file == file }) {
                logger.warn("Font file ${file.absolutePath} is already loaded.")
                return
            }

            val font = file.createFont().deriveFont(DEFAULT_FONT_SIZE)

            // Name will consist of the font name and family. This makes it possible
            // to select the different styles of the font.
            val fontFace = FontFace(font.name, DEFAULT_FONT_SIZE, file)
            fontFace.fillDerivedStyles(font)
            addFontFace(fontFace)
        } catch (e: Exception) {
            logger.warn("Failed to load font from file ${file.absolutePath}", e)
        }
    }

    suspend fun queueFontFromStream(stream: InputStream) {
        val font = stream.createFont().deriveFont(DEFAULT_FONT_SIZE)
        val fontFace = FontFace(font.name, DEFAULT_FONT_SIZE, file = null)
        fontFace.fillDerivedStyles(font)
        addFontFace(fontFace)
    }

    private suspend fun systemFont(name: String): FontFace {
        val fontFace = FontFace(name, DEFAULT_FONT_SIZE)

        STYLES.forEach { style ->
            val font = Font(name, style, DEFAULT_FONT_SIZE.toInt())
            fontFace.fillStyle(font, style)
        }

        return fontFace
    }

}
