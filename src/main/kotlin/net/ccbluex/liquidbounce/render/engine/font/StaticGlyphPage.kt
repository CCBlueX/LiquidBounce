package net.ccbluex.liquidbounce.render.engine.font

import net.ccbluex.liquidbounce.render.engine.font.BaseGlpyhPage.Companion.CharacterGenerationInfo
import net.minecraft.client.texture.NativeImageBackedTexture
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A staticly allocated glyph page.
 */
class StaticGlyphPage(
    override val texture: NativeImageBackedTexture,
    val glyphs: Map<Char, Glyph>,
    val height: Float,
    val ascent: Float,
    override val fallbackGlyph: Glyph
): BaseGlpyhPage() {
    companion object {
        /**
         * Creates a glyph page containing all ASCII characters
         */
        fun createAscii(font: Font) = create('\u0000'..'\u00FF', font)

        /**
         * Creates a bitmap based
         */
        fun create(chars: Iterable<Char>, font: Font): StaticGlyphPage {
            // Get information about the glyphs and sort them by their height
            val glyphsToRender = chars
                .mapNotNull { createCharacterCreationInfo(it, font) }
                .sortedBy { it.glyphMetrics.bounds2D.height }

            val maxTextureSize = maxTextureSize.value

            // The suggested width of the atlas, determined by a simple heuristic, capped by the maximal texture size
            val totalArea =
                glyphsToRender.sumOf { it.glyphMetrics.bounds2D.width * it.glyphMetrics.bounds2D.height }

            val suggestedAtlasWidth = min(
                (sqrt(totalArea) * 1.232).toInt(),
                maxTextureSize
            )

            // Do the placement
            val atlasDimensions = doCharacterPlacement(glyphsToRender, suggestedAtlasWidth)

            check(atlasDimensions.width <= maxTextureSize && atlasDimensions.height <= maxTextureSize) {
                "Multiple atlases are not implemented yet."
            }

            val (atlas, fontMetrics) = renderGlyphs(
                createBufferedImageWithDimensions(atlasDimensions),
                font, glyphsToRender
            )

            val glyphs = glyphsToRender.map { createGlyphFromGenerationInfo(it, atlasDimensions) }

            val map = HashMap<Char, Glyph>(glyphs.size)

            for (glyph in glyphs) {
                map[glyph.char] = glyph
            }

            val nativeImage = atlas.toNativeImage()
            val texture = NativeImageBackedTexture(nativeImage)

            texture.bindTexture()
            texture.image!!.upload(0, 0, 0, 0, 0, nativeImage.width, nativeImage.height, true, false, true, false)

            return StaticGlyphPage(
                texture,
                map,
                fontMetrics.height.toFloat(),
                fontMetrics.ascent.toFloat(),
                map[font.missingGlyphCode.toChar()] ?: map['?'] ?: error("No fallback glyph found")
            )
        }

        /**
         * Used for [create]. Assigns a position to every glyph.
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
                if (glyph.glyphMetrics.isWhitespace) {
                    continue
                }

                // 1px padding to prevent stuff from happening
                val allocationSize = glyph.atlasDimension

                // Would the character be longer than the atlas?
                if (currentX + allocationSize.width >= atlasWidth) {
                    currentX = 0
                    currentY += currentLineMaxHeight
                    currentLineMaxHeight = 0
                }

                // Update max width
                if (currentX + allocationSize.width > maxWidth) {
                    maxWidth = currentX + allocationSize.width
                }

                // Update currentLineMaxHeight
                if (allocationSize.height > currentLineMaxHeight) {
                    currentLineMaxHeight = allocationSize.height
                }

                // Do the placement
                glyph.atlasLocation = Point(currentX, currentY)

                currentX += allocationSize.width
            }

            // Return the dimension and match it's requirement of being at least (1, 1)
            return Dimension(max(1, maxWidth), max(1, currentY + currentLineMaxHeight))
        }
    }

    override fun getGlyph(char: Char): Glyph? {
        return this.glyphs.get(char)
    }

}
