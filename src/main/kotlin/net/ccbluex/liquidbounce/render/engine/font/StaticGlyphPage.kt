package net.ccbluex.liquidbounce.render.engine.font

import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.GlyphPage.Companion.CharacterGenerationInfo
import net.ccbluex.liquidbounce.utils.client.logger
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
    val glyphs: Set<Pair<FontManager.FontId, GlyphRenderInfo>>
): GlyphPage() {
    companion object {
        fun createGlyphPages(chars: List<FontGlyph>): List<StaticGlyphPage> {
            val glyphPages = mutableListOf<StaticGlyphPage>()

            var remainingChars = chars

            do {
                val result = createGlyphPageWithFittingCharacters(remainingChars)

                glyphPages.add(result.first)

                remainingChars = result.second
            } while (remainingChars.isNotEmpty())

            return glyphPages
        }

        /**
         * Creates a bitmap which contains all [chars].
         */
        fun createGlyphPageWithFittingCharacters(chars: List<FontGlyph>): Pair<StaticGlyphPage, List<FontGlyph>> {
            val result: Pair<GlyphPlacementResult, List<FontGlyph>>? = tryCharacterPlacementWithShrinking(chars)

            val (res, remainingGlyphs) = result ?: error("Unable to create static atlas.")

            if (res.glyphsToRender.size < chars.size) {
                logger.warn("Failed to place all characters (${chars.size}) on the atlas, " +
                        "using a reduced charset (${res.glyphsToRender.size}) instead!")
            }

            return renderGlyphPage(res) to remainingGlyphs
        }

        /**
         * Tries to fit all characters on a page.
         * If it does not fit, it reduces the list of characters to place by 20% and retries.
         */
        private fun tryCharacterPlacementWithShrinking(
            chars: List<FontGlyph>
        ): Pair<GlyphPlacementResult, List<FontGlyph>>? {
            var currentLen = chars.size

            while (currentLen > 1) {
                val result = tryCharacterPlacement(chars.subList(0, currentLen))

                if (result != null) {
                    return result to chars.subList(currentLen, chars.size)
                }

                currentLen = currentLen * 4 / 5
            }

            return null
        }

        private fun renderGlyphPage(placementPlan: GlyphPlacementResult): StaticGlyphPage {
            val atlas = createBufferedImageWithDimensions(placementPlan.atlasDimension)

            renderGlyphs(atlas, placementPlan.glyphsToRender)

            val glyphs = placementPlan.glyphsToRender
                .map { it.fontGlyph.font to createGlyphFromGenerationInfo(it, placementPlan.atlasDimension) }
                .toSet()

            val nativeImage = atlas.toNativeImage()
            val texture = NativeImageBackedTexture(nativeImage)

            texture.bindTexture()
            texture.image!!.upload(0, 0, 0, 0, 0, nativeImage.width, nativeImage.height, true)

            return StaticGlyphPage(
                texture,
                glyphs,
            )
        }

        /**
         * Tries to come up with a placement which includes all [chars].
         *
         * @return null if the resulting atlas is bigger than the maximum texture size.
         */
        private fun tryCharacterPlacement(chars: List<FontGlyph>): GlyphPlacementResult? {
            // Get information about the glyphs and sort them by their height
            val glyphsToRender = chars
                .mapNotNull { createCharacterCreationInfo(it) }
                .sortedBy { it.glyphMetrics.bounds2D.height }

            val maxTextureSize = maxTextureSize.value

            // The suggested width of the atlas, determined by a simple heuristic, capped by the maximal texture size
            val totalArea = glyphsToRender.sumOf { it.glyphMetrics.bounds2D.width * it.glyphMetrics.bounds2D.height }

            val suggestedAtlasWidth = min(
                (sqrt(totalArea) * 1.232).toInt(),
                maxTextureSize
            )

            // Do the placement
            val atlasDimensions = placeCharacters(glyphsToRender, suggestedAtlasWidth)

            // The placement won't fit on the current atlas size.
            if (atlasDimensions.width > maxTextureSize || atlasDimensions.height > maxTextureSize) {
                return null
            }

            return GlyphPlacementResult(glyphsToRender, atlasDimensions)
        }

        /**
         * Used for [createGlyphPageWithFittingCharacters]. Assigns a position to every glyph.
         *
         * @param atlasWidth The width of the atlas. No character will be longer that this width
         *
         * @return The height of the resulting texture. Is at least (1, 1)
         */
        private fun placeCharacters(glyphs: List<CharacterGenerationInfo>, atlasWidth: Int): Dimension {
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

    private class GlyphPlacementResult(val glyphsToRender: List<CharacterGenerationInfo>, val atlasDimension: Dimension)
}
