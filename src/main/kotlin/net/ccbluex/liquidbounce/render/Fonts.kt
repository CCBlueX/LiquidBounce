/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.HttpClient.download
import java.awt.Font
import java.io.File
import java.io.FileInputStream

/**
 * Font loader
 */
object Fonts {

    val loadedFonts = mutableMapOf<FontDetail, AbstractFontRenderer>()

    private val fontsRoot = File(ConfigSystem.rootFolder, "fonts").apply {
        if (!exists()) {
            mkdir()
        }
    }

    private object Options : Configurable("Fonts") {

        val fonts by value(
            "Fonts",
            arrayOf(
                // Default fonts
                FontDetail("Roboto-Medium.ttf", 40),
                FontDetail("Roboto-Bold.ttf", 180)
            )
        )

    }

    // todo: replace by font selectors
    val bodyFont: AbstractFontRenderer
        get() = loadedFonts.values.first()

    init {
        ConfigSystem.root(Options)
    }

    fun loadFonts() {
        Options.fonts.forEach { font ->
            loadedFonts[font] = font.loadFont()
        }
    }

    data class FontDetail(val name: String, val size: Int) {

        fun loadFont(): AbstractFontRenderer {
            val file = File(fontsRoot, name)

            if (!file.exists()) {
                // Try to download font. Might be in our cloud.
                downloadFont()
            }

            return FontRenderer.createFontRenderer(getAwtFont())
        }

        private fun downloadFont() {
            runCatching {
                logger.info("Downloading required font $name...")
                download("${LiquidBounce.CLIENT_CLOUD}/fonts/$name", File(fontsRoot, name))
            }.onFailure {
                logger.error("Unable to download font $name!", it)
            }
        }

        private fun getAwtFont() = runCatching {
            FileInputStream(File(fontsRoot, name)).use {
                Font.createFont(Font.TRUETYPE_FONT, it).deriveFont(Font.PLAIN, size.toFloat())
            }
        }.onFailure {
            logger.error("Unable to load font $name. Falling back to default.", it)
        }.getOrElse { Font("default", Font.PLAIN, size) }

    }

}
