package net.ccbluex.liquidbounce.renderer.engine

import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.GlyphMetrics
import java.awt.font.LineMetrics
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class BoundingBox2D(val xMin: Float, val yMin: Float, val xMax: Float, val yMax: Float) {
    constructor(rect: Rectangle2D) : this(
        rect.x.toFloat(),
        rect.y.toFloat(),
        rect.width.toFloat(),
        rect.height.toFloat()
    )
}

/**
 * Contains information about the placement of characters in a bitmap
 * and how they are rendered
 */
data class Glyph(
    /**
     * Is this character a whitespace? If it is, there is no texture
     * for this glyph. The renderer is instructed to just advance
     */
    val isWhitespace: Boolean,
    /**
     * The location of the Glyph on the sprite, might be null if [isWhitespace]
     * is true
     */
    val atlasLocation: BoundingBox2D?,
    /**
     * The bounds of the rendered glyph. Used for rendering.
     */
    val glyphBounds: BoundingBox2D,
    val useHorizontalBaseline: Boolean,
    val advanceX: Float,
    val advanceY: Float
)

class GlyphPage(val image: BufferedImage, val glyphs: Array<Glyph>) {

    companion object {
        /**
         * The max width and height a texture can have.
         *
         * *Only* request this field's value from a thread with an OpenGL context
         */
        val maxTextureSize = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            // As specified in the OpenGL reference, GL_MAX_TEXTURE_SIZE must be at least 1024.
            // If it is less than that, an error occurred, the 1024 is just a failsafe.
//            max(GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE), 1024)
            4096
        }

        /**
         * Used for the Font Atlas generation
         */
        class CharacterGenerationInfo(val c: Char, val glyphMetrics: GlyphMetrics, val lineMetrics: LineMetrics) {
            lateinit var atlasLocation: Point
        }

        /**
         * Creates a bitmap based
         */
        fun createBitmap(chars: Iterable<Char>, font: Font): GlyphPage {
            val fontRendererContext = FontRenderContext(AffineTransform(), true, true)

            // Get information about the glyphs and sort them by their height
            val glyphsToRender = chars
                .filter(font::canDisplay)
                .map {
                    val charString = it.toString()
                    val glyphVector = font.createGlyphVector(fontRendererContext, charString)

                    val lineMetrics = font.getLineMetrics(charString, fontRendererContext)
                    val glyph = glyphVector.getGlyphMetrics(0)

                    CharacterGenerationInfo(it, glyph, lineMetrics)
                }
                .sortedBy { it.glyphMetrics.bounds2D.height }

            val maxTextureSize = maxTextureSize.value

            // The suggested width of the atlas, determined by a simple heuristic, capped by the maximal texture size
            val suggestedAtlasWidth = min(
                (sqrt(glyphsToRender.sumByDouble { it.glyphMetrics.bounds2D.width * it.glyphMetrics.bounds2D.height } * 1.2) * 1.125).toInt(),
                maxTextureSize
            )

            // Do the placement
            val atlasDimensions = doCharacterPlacement(glyphsToRender, suggestedAtlasWidth)

            if (atlasDimensions.width > maxTextureSize || atlasDimensions.height > maxTextureSize)
                TODO("Implement multiple atlases.")

            // Allocate the atlas texture
            val atlas = BufferedImage(atlasDimensions.width, atlasDimensions.height, BufferedImage.TYPE_INT_RGB)
            val atlasGraphics = atlas.createGraphics()

            // Enable font anti aliasing
            atlasGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            // Make the graphics object use the specified font
            atlasGraphics.font = font

            for (characterInfo in glyphsToRender) {
                // Whitespaces are not placed, so they are not rendered
                if (characterInfo.glyphMetrics.isWhitespace)
                    continue

                // Draw the character to the atlas, offset by start of the character
                atlasGraphics.drawString(
                    characterInfo.c.toString(),
                    characterInfo.atlasLocation.x - characterInfo.glyphMetrics.bounds2D.x.toInt(),
                    characterInfo.atlasLocation.y - characterInfo.glyphMetrics.bounds2D.y.toInt()
                )
            }

            atlasGraphics.dispose()

            // Precalculate the reciprocal values to make the thing faster
            val textureWidthMultiplier = 1.0f / atlasDimensions.width.toFloat()
            val textureHeightMultiplier = 1.0f / atlasDimensions.height.toFloat()

            val glyphs = glyphsToRender.map {
                val atlasLocation = if (it.glyphMetrics.isWhitespace)
                    null
                else {
                    val x = it.atlasLocation.x.toFloat()
                    val y = it.atlasLocation.x.toFloat()

                    BoundingBox2D(
                        x * textureWidthMultiplier,
                        y * textureHeightMultiplier,
                        (x + it.glyphMetrics.bounds2D.width.toFloat()) * textureWidthMultiplier,
                        (y + it.glyphMetrics.bounds2D.height.toFloat()) * textureHeightMultiplier
                    )
                }

                Glyph(
                    it.glyphMetrics.isWhitespace,
                    atlasLocation,
                    BoundingBox2D(it.glyphMetrics.bounds2D),
                    false, // TODO Find this out
                    it.glyphMetrics.advanceX,
                    it.glyphMetrics.advanceY,
                )
            }

            return GlyphPage(atlas, glyphs.toTypedArray())
        }

        /**
         * Used for [createBitmap]. Assigns a position to every glyph.
         *
         * @param atlasWidth The width of the atlas. No character will be longer that this width
         *
         * @return The height of the resulting texture. Is at least (1, 1)
         */
        private fun doCharacterPlacement(glyphs: List<CharacterGenerationInfo>, atlasWidth: Int): Dimension {
            var currentX = 0
            var currentY = 0

            // The highest pixel that is allocated.
            var maxWidth = 0

            // The height of the highest character in the currently placed line.
            var currentLineMaxHeight = 0

            for (glyph in glyphs) {
                // Whitespaces don't need to be placed
                if (glyph.glyphMetrics.isWhitespace)
                    continue

                val glyphWidth = ceil(glyph.glyphMetrics.bounds2D.width).toInt()
                val glyphHeight = ceil(glyph.glyphMetrics.bounds2D.height).toInt()

                // Would the character be longer than the atlas?
                if (currentX + glyphWidth >= atlasWidth) {
                    currentX = 0
                    currentY += currentLineMaxHeight
                    currentLineMaxHeight = 0
                }

                // Update max width
                if (currentX + glyphWidth > maxWidth)
                    maxWidth = currentX + glyphWidth

                // Update currentLineMaxHeight
                if (glyphHeight > currentLineMaxHeight)
                    currentLineMaxHeight = glyphHeight

                // Do the placement
                glyph.atlasLocation = Point(currentX, currentY)

                currentX += glyphWidth
            }

            // Return the dimension and match it's requirement of being at least (1, 1)
            return Dimension(max(1, maxWidth), max(1, currentY + currentLineMaxHeight))
        }
    }


}
