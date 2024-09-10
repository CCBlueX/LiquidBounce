package net.ccbluex.liquidbounce.render.engine.font

import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.engine.font.GlyphPage.Companion.CharacterGenerationInfo
import net.minecraft.client.texture.NativeImageBackedTexture
import java.awt.Dimension
import java.awt.Point
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A staticly allocated glyph page.
 */
class StaticGlyphPage(
    override val texture: NativeImageBackedTexture,
    val glyphs: List<Pair<Fonts.FontId, GlyphRenderInfo>>
): GlyphPage() {
    companion object {
        /**
         * Creates a bitmap based
         */
        fun create(chars: List<FontGlyph>): StaticGlyphPage {
            // Get information about the glyphs and sort them by their height
            val glyphsToRender = chars
                .mapNotNull { createCharacterCreationInfo(it) }
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

            // TODO: Multi atlas support

            val atlas = createBufferedImageWithDimensions(atlasDimensions)

            renderGlyphs(atlas, glyphsToRender)

            val glyphs = glyphsToRender.map { it.fontGlyph.font to createGlyphFromGenerationInfo(it, atlasDimensions) }

            val nativeImage = atlas.toNativeImage()
            val texture = NativeImageBackedTexture(nativeImage)

            texture.bindTexture()
            texture.image!!.upload(0, 0, 0, 0, 0, nativeImage.width, nativeImage.height, true, false, true, false)

            return StaticGlyphPage(
                texture,
                glyphs,
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

                val allocationSize = glyph.atlasDimension

                // Would the character be longer than the atlas?
                if (currentX + allocationSize.width >= atlasWidth) {
                    currentX = 0
                    currentY += currentLineMaxHeight
                    currentLineMaxHeight = 0
                }

                maxWidth = max(maxWidth, currentX + allocationSize.width)
                currentLineMaxHeight = max(currentLineMaxHeight, allocationSize.height)

                // Do the placement
                glyph.atlasLocation = Point(currentX, currentY)

                currentX += allocationSize.width
            }

            // Return the dimension and match it's requirement of being at least (1, 1)
            return Dimension(max(1, maxWidth), max(1, currentY + currentLineMaxHeight))
        }
    }

}
