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

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.AsyncLazy
import net.ccbluex.liquidbounce.config.types.ChooseListValue
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.render.engine.font.FontGlyphPageManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.createFont
import net.minecraft.util.Util
import net.minecraft.util.Util.OS.LINUX
import net.minecraft.util.Util.OS.OSX
import net.minecraft.util.Util.OS.WINDOWS
import java.awt.Font
import java.io.File
import java.io.InputStream

object FontManager {

    private val FONT_VALUES = mutableListOf<ChooseListValue<FontFace>>()

    /**
     * Creates a font value. The choices will be sync with [fontFaces].
     */
    fun Configurable.font(name: String, default: FontFace = COMMON_FONT) =
        enumChoice(name, default, fontFaces.values).apply {
            FONT_VALUES += this
            doNotIncludeAlways()
        }

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
                WINDOWS -> queueSystemFont("Segoe UI")
                OSX -> queueSystemFont("Helvetica")
                LINUX -> queueSystemFont("DejaVu Sans")
                else -> queueSystemFont("Arial")
            }
        }.onFailure { throwable ->
            logger.error("Failed to load common font.", throwable)
        }.getOrNull() ?: queueSystemFont("Arial")
    }

    /**
     * Default font for displaying CJK (Chinese, Japanese, Korean) characters.
     */
    private val CJK_FONT by AsyncLazy {
        runCatching {
            when (Util.getPlatform()) {
                WINDOWS -> queueSystemFont("Microsoft YaHei")
                OSX -> queueSystemFont("PingFang SC")
                LINUX -> queueSystemFont("Noto Sans CJK")
                else -> null // No default CJK font available
            }
        }.onFailure { throwable ->
            logger.error("Failed to load CJK font.", throwable)
        }.getOrNull()
    }

    /**
     * All font faces that are known to the font manager.
     *
     * Use [BiMap] because the [BiMap.values] is [Set]. It's not allowed to add same font.
     *
     * Note: always add with [addFontFace]!
     */
    internal val fontFaces: BiMap<String, FontFace>
        field = HashBiMap.create<String, FontFace>()

    private fun addFontFace(fontFace: FontFace) = mc.execute {
        fontFaces[fontFace.name] = fontFace
        FONT_VALUES.forEach { it.choices = fontFaces.values }
        ModuleClickGui.reload()
    }

    /**
     * Returns the font by the given name.
     */
    internal fun fontFace(name: String) = fontFaces[name]

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
        _glyphManager = FontGlyphPageManager(
            baseFonts = fontFaces.values,
            additionalFonts = setOfNotNull(CJK_FONT)
            // TODO: CJK and COMMON is now base instead of additional (queueSystemFont)
        )
    }

    suspend fun queueFontFromFile(file: File): FontFace? {
        return try {
            if (!file.exists()) {
                logger.warn("Font file ${file.absolutePath} does not exist.")
                return null
            }

            if (file.extension.equals("ttf", ignoreCase = true)) {
                logger.warn("Font file ${file.absolutePath} is not a TrueType font.")
                return null
            }

            if (fontFaces.values.any { it.file == file }) {
                logger.warn("Font file ${file.absolutePath} is already loaded.")
                return null
            }

            val font = file.createFont().deriveFont(DEFAULT_FONT_SIZE)

            // Name will consist of the font name and family. This makes it possible
            // to select the different styles of the font.
            val fontFace = FontFace(font.name, DEFAULT_FONT_SIZE, file)
            // In this case, we have only one style available, which is the plain style.
            fontFace.fillStyle(font, Font.PLAIN)
            fontFace.also(::addFontFace)
        } catch (e: Exception) {
            logger.warn("Failed to load font from file ${file.absolutePath}", e)
            null
        }
    }

    suspend fun queueFontFromStream(stream: InputStream): FontFace {
        val font = stream.createFont().deriveFont(DEFAULT_FONT_SIZE)
        val fontFace = FontFace(font.name, DEFAULT_FONT_SIZE, file = null)
        fontFace.fillStyle(font, Font.PLAIN)
        return fontFace.also(::addFontFace)
    }

    suspend fun queueSystemFont(name: String): FontFace {
        val fontFace = FontFace(name, DEFAULT_FONT_SIZE)

        STYLES.forEach { style ->
            val font = Font(name, style, DEFAULT_FONT_SIZE.toInt())
            fontFace.fillStyle(font, style)
        }

        return fontFace.also(::addFontFace)
    }

}
