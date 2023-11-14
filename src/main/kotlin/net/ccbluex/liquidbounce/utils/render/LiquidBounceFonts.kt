package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.engine.font.GlyphPage
import java.awt.Font
import java.io.File

object LiquidBounceFonts {
    const val DEFAULT_FONT_SIZE: Int = 43

    val DEFAULT_FONT: FontRenderer

    private val fontFiles = arrayOf(
        "regular.ttf",
        "bold.ttf",
        "italic.ttf",
        "bold-italic.ttf"
    )

    init {
        val basePath = ConfigSystem.rootFolder.resolve("themes/default/hud/font/")

        DEFAULT_FONT = createFontFromFolder(basePath)
    }

    private fun createFontFromFolder(basePath: File): FontRenderer {
        try {
            return FontRenderer(
                fontFiles.map {
                    val font = Font
                        .createFont(Font.TRUETYPE_FONT, basePath.resolve(it))
                        .deriveFont(DEFAULT_FONT_SIZE.toFloat())

                    GlyphPage.createAscii(font)
                }.toTypedArray(),
                DEFAULT_FONT_SIZE.toFloat()
            )
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load font from folder $basePath", e)
        }
    }
}
