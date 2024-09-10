/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.render.engine.font.FontGlyphPageManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.utils.client.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.HttpClient.download
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.validation.HashValidator
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File

/**
 * Font loader
 */
object Fonts {

    // .minecraft/LiquidBounce/fonts
    private val fontsRoot = ConfigSystem.rootFolder.resolve("fonts").apply {
        if (!exists()) {
            mkdir()
        }
    }

    private var fontQueue = mutableListOf<QueuedFont>()

    const val DEFAULT_FONT_SIZE: Int = 43
    val FONT_FORMATS = arrayOf("Regular", "Bold", "Italic", "BoldItalic")
    val DEFAULT_FONT = FontLoadingInfo("Montserrat")
        .queueLoad()

    private var glyphManager: FontGlyphPageManager? = null

    fun getGlyphPageManager(): FontGlyphPageManager {
        return this.glyphManager ?: error("Glyph manager was not initialized yet!")
    }

    fun loadQueuedFonts() {
        this.glyphManager?.unload()

        fontQueue.forEach {
            logger.info("Loading queued font ${it.fontLoadingInfo.name}")
            it.loadNow()
        }

        this.glyphManager = FontGlyphPageManager(fontQueue.map { it.get() })
    }

    /**
     * Takes the name of the font and loads it from the filesystem
     * or downloads it from the cloud.
     */
    data class FontLoadingInfo(val name: String) {

        /**
         * Loads the font from the filesystem or downloads it from the cloud and
         * creates a [FontRenderer] instance from it.
         */
        internal fun load(retry: Boolean = true): LoadedFont {
            val file = File(fontsRoot, name)

            HashValidator.validateFolder(file)

            if (!file.exists() || !file.isDirectory || file.listFiles().isNullOrEmpty()) {
                runCatching {
                    downloadFont()
                }.onFailure {
                    logger.error("Failed to download font $name", it)
                    ErrorHandler.fatal(it, "Failed to download font $name")
                }
            }

            return runCatching {
                createFontFromFolder(file)
            }.getOrElse {
                logger.error("Failed to load font $name", it)

                // Might retry to download the font if it's corrupted
                file.deleteRecursively()
                if (retry) {
                    return load(retry = false)
                }

                error("Failed to load font $name")
            }
        }

        /**
         * Downloads the font from the cloud and extracts it to the filesystem.
         */
        private fun downloadFont() {
            logger.info("Downloading required font $name...")

            val fontFolder = fontsRoot.resolve(name).apply {
                if (exists()) {
                    deleteRecursively()
                }

                mkdir()
            }

            val fontZip = fontFolder.resolve("font.zip")
            logger.info("Downloading font $name to $fontZip")
            download("${LiquidBounce.CLIENT_CLOUD}/fonts/$name.zip", fontZip)

            logger.info("Extracting font $name to $fontFolder")
            extractZip(fontZip, fontFolder)
            fontZip.delete()

            logger.info("Successfully downloaded font $name")
        }

        /**
         * Creates a [FontRenderer] instance from the given folder.
         */
        private fun createFontFromFolder(basePath: File): LoadedFont {
            try {
                return LoadedFont(
                    DEFAULT_FONT_SIZE.toFloat(),
                    FONT_FORMATS.mapIndexed { idx, formatName ->
                        val font = Font
                            .createFont(Font.TRUETYPE_FONT, basePath.resolve("$name-$formatName.ttf"))
                            .deriveFont(DEFAULT_FONT_SIZE.toFloat())

                        val metrics =
                            BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics().apply {
                                setFont(font)
                            }.fontMetrics

                        FontId(idx, font, metrics.height.toFloat(), metrics.ascent.toFloat())
                    }.toTypedArray(),
                )
            } catch (e: Exception) {
                throw IllegalStateException("Failed to load font from folder $basePath", e)
            }
        }

        fun queueLoad() = QueuedFont(this).also { fontQueue += it }

    }

    data class QueuedFont(val fontLoadingInfo: FontLoadingInfo) {
        private var font: LoadedFont? = null

        fun get() = font ?: error("Font was not loaded yet!")

        fun loadNow() {
            if (font != null) {
                return
            }

            font = fontLoadingInfo.load()
        }

    }

    class LoadedFont(
        val fontSize: Float,
        val styles: Array<FontId?>,
    )

    class FontId(
        val style: Int,
        val awtFont: Font,
        val height: Float,
        val ascent: Float
    )

}
