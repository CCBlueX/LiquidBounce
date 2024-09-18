package net.ccbluex.liquidbounce.render.engine.font

import java.awt.Dimension
import java.awt.Font
import java.awt.image.BufferedImage
import kotlin.math.ceil

class FontGlyphPageManager(
    font: Font
) {
    var staticPage: StaticGlyphPage
    private val dynamicPage: DynamicGlyphPage
    val height: Float
    val ascent: Float

    init {
        val fontMetrics =
            BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB).createGraphics().apply { setFont(font) }.fontMetrics

        this.height = fontMetrics.height.toFloat()
        this.ascent = fontMetrics.ascent.toFloat()

        this.dynamicPage = DynamicGlyphPage(Dimension(1024, 1024), ceil(this.height).toInt())
        this.staticPage = StaticGlyphPage.create('\u0000'..'\u0600', font)
    }

}
