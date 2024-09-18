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
    val DEFAULT_FONT = FontInfo("Montserrat")
        .queueLoad()

    fun loadQueuedFonts() {
        fontQueue.forEach {
            logger.info("Loading queued font ${it.fontInfo.name}")
            it.loadNow()
        }
        fontQueue.clear()
    }

    /**
     * Takes the name of the font and loads it from the filesystem
     * or downloads it from the cloud.
     */
    data class FontInfo(val name: String) {

        /**
         * Loads the font from the filesystem or downloads it from the cloud and
         * creates a [FontRenderer] instance from it.
         */
        internal fun load(retry: Boolean = true): FontRenderer {
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
        private fun createFontFromFolder(basePath: File): FontRenderer {
            try {
                return FontRenderer(
                    FONT_FORMATS.map {
                        val font = Font
                            .createFont(Font.TRUETYPE_FONT, basePath.resolve("$name-$it.ttf"))
                            .deriveFont(DEFAULT_FONT_SIZE.toFloat())


                        FontGlyphPageManager(font)
                    }.toTypedArray(),
                    DEFAULT_FONT_SIZE.toFloat()
                )
            } catch (e: Exception) {
                throw IllegalStateException("Failed to load font from folder $basePath", e)
            }
        }

        fun queueLoad() = QueuedFont(this).also { fontQueue += it }

    }

    data class QueuedFont(val fontInfo: FontInfo) {

        private var fontRenderer: FontRenderer? = null

        fun get() = fontRenderer ?: error("Font was not loaded yet!")

        fun loadNow() {
            if (fontRenderer != null) {
                return
            }

            fontRenderer = fontInfo.load()
        }

    }

}
