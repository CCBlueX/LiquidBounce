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
package net.ccbluex.liquidbounce.render.engine.font

import net.ccbluex.liquidbounce.render.engine.UV2f
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import org.lwjgl.opengl.GL11
import java.awt.*
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

data class BoundingBox2f(val xMin: Float, val yMin: Float, val xMax: Float, val yMax: Float) {
    constructor(rect: Rectangle2D) : this(
        rect.x.toFloat(),
        rect.y.toFloat(),
        rect.width.toFloat(),
        rect.height.toFloat()
    )
}

data class BoundingBox2s(val min: UV2f, val max: UV2f) {
    constructor(rect: BoundingBox2f) : this(
        UV2f(
            rect.xMin,
            rect.yMin
        ),
        UV2f(
            rect.xMax,
            rect.yMax
        )
    )
}

/**
 * Contains information about the placement of characters in a bitmap
 * and how they are rendered
 */
data class Glyph(
    /**
     * Which char does this glyph represent?
     */
    val char: Char,
    /**
     * Is this character a whitespace? If it is, there is no texture
     * for this glyph. The renderer is instructed to just advance
     */
    val isWhitespace: Boolean,
    /**
     * The location of the Glyph on the sprite, might be null if [isWhitespace]
     * is true
     */
    val atlasLocation: BoundingBox2s?,
    /**
     * The location of the Glyph on the sprite, might be null if [isWhitespace]
     * is true
     */
    val atlasWidth: Float,
    val atlasHeight: Float,
    /**
     * The bounds of the rendered glyph. Used for rendering.
     */
    val glyphBounds: BoundingBox2f,
    val useHorizontalBaseline: Boolean,
    val advanceX: Float,
    val advanceY: Float
)

class GlyphPage(
    val texture: NativeImageBackedTexture,
    val glyphs: Map<Char, Glyph>,
    val height: Float,
    val ascent: Float
) {
    companion object {
        /**
         * The max width and height a texture can have.
         *
         * *Only* request this field's value from a thread with an OpenGL context
         */
        private val maxTextureSize = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            // As specified in the OpenGL reference, GL_MAX_TEXTURE_SIZE must be at least 1024.
            // If it is less than that, an error occurred, the 1024 is just a failsafe.
            max(GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE), 1024)
        }

        /**
         * Initializes the static values for glyph pages. Has to be called from a thread with an OpenGL context.
         */
        fun init() {
            maxTextureSize.value
        }

        /**
         * Used for the Font Atlas generation
         */
        class CharacterGenerationInfo(val c: Char, val glyphMetrics: GlyphMetrics, val lineMetrics: LineMetrics) {
            lateinit var atlasLocation: Point
        }


        /**
         * Creates a glyph page containing all ASCII characters
         */
        fun createAscii(font: Font) = create('\u0000'..'\u00FF', font)

        /**
         * Creates a bitmap based
         */
        fun create(chars: Iterable<Char>, font: Font): GlyphPage {
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

            val (atlas, fontMetrics) = renderAtlas(atlasDimensions, font, glyphsToRender)

            val glyphs = glyphsToRender.map { createGlyphFromGenerationInfo(it, atlasDimensions) }

            val map = HashMap<Char, Glyph>(glyphs.size)

            for (glyph in glyphs) {
                map[glyph.char] = glyph
            }

            val nativeImage = createNativeImage(atlas)
            val texture = NativeImageBackedTexture(nativeImage)

            texture.bindTexture()
            texture.image!!.upload(0, 0, 0, 0, 0, nativeImage.width, nativeImage.height, true, false, true, false)

            return GlyphPage(
                texture,
                map,
                fontMetrics.height.toFloat(),
                fontMetrics.ascent.toFloat()
            )
        }

        private fun createNativeImage(atlas: BufferedImage): NativeImage {
            val nativeImage = NativeImage(NativeImage.Format.RGBA, atlas.width, atlas.height, false)

            // Fuck Minecraft native image
            for (x in 0 until atlas.width) {
                for (y in 0 until atlas.height) {
                    nativeImage.setColor(x, y, atlas.getRGB(x, y))
                }
            }

            return nativeImage
        }

        private fun createGlyphFromGenerationInfo(
            it: CharacterGenerationInfo,
            atlasDimensions: Dimension
        ): Glyph {
            val atlasLocation = if (it.glyphMetrics.isWhitespace) {
                null
            } else {
                val x = it.atlasLocation.x.toFloat()
                val y = it.atlasLocation.y.toFloat()

                BoundingBox2f(
                    x,
                    y,
                    (x + ceil(it.glyphMetrics.bounds2D.width.toFloat()) + 1.0f),
                    (y + ceil(it.glyphMetrics.bounds2D.height.toFloat()) + 1.0f)
                )
            }

            // Precalculate the reciprocal values to make the thing faster
            val textureWidthMultiplier = 1.0f / atlasDimensions.width.toFloat()
            val textureHeightMultiplier = 1.0f / atlasDimensions.height.toFloat()

            return Glyph(
                it.c,
                it.glyphMetrics.isWhitespace,
                atlasLocation?.let { bb ->
                    BoundingBox2s(
                        BoundingBox2f(
                            bb.xMin * textureWidthMultiplier,
                            bb.yMin * textureHeightMultiplier,
                            bb.xMax * textureWidthMultiplier,
                            bb.yMax * textureHeightMultiplier
                        )
                    )
                },
                atlasLocation?.let { bb -> bb.xMax - bb.xMin } ?: 0.0f,
                atlasLocation?.let { bb -> bb.yMax - bb.yMin } ?: 0.0f,
                BoundingBox2f(it.glyphMetrics.bounds2D),
                false, // TODO Find this out
                it.glyphMetrics.advanceX,
                it.glyphMetrics.advanceY
            )
        }

        private fun renderAtlas(
            atlasDimensions: Dimension,
            font: Font,
            glyphsToRender: List<CharacterGenerationInfo>
        ): Pair<BufferedImage, FontMetrics> {
            // Allocate the atlas texture
            val atlas = BufferedImage(atlasDimensions.width, atlasDimensions.height, BufferedImage.TYPE_INT_ARGB)
            val atlasGraphics = atlas.createGraphics()

            // Enable font anti aliasing
            atlasGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            atlasGraphics.paint = Color(255, 255, 255, 0)
            atlasGraphics.fillRect(0, 0, atlas.width, atlas.height)
            atlasGraphics.paint = Color.white

            // Make the graphics object use the specified font
            atlasGraphics.font = font

            // Draw glyphs onto the image
            for (characterInfo in glyphsToRender) {
                // Whitespaces are not placed, so they are not rendered
                if (characterInfo.glyphMetrics.isWhitespace) {
                    continue
                }

                // Draw the character to the atlas, offset by start of the character + a pixel padding
                atlasGraphics.drawString(
                    characterInfo.c.toString(),
                    characterInfo.atlasLocation.x - characterInfo.glyphMetrics.bounds2D.x.toInt() + 1,
                    characterInfo.atlasLocation.y - characterInfo.glyphMetrics.bounds2D.y.toInt() + 1
                )
            }

            atlasGraphics.dispose()

            val fontMetrics = atlasGraphics.fontMetrics
            return Pair(atlas, fontMetrics)
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
                val glyphWidth = ceil(glyph.glyphMetrics.bounds2D.width).toInt() + 2
                val glyphHeight = ceil(glyph.glyphMetrics.bounds2D.height).toInt() + 2

                // Would the character be longer than the atlas?
                if (currentX + glyphWidth >= atlasWidth) {
                    currentX = 0
                    currentY += currentLineMaxHeight
                    currentLineMaxHeight = 0
                }

                // Update max width
                if (currentX + glyphWidth > maxWidth) {
                    maxWidth = currentX + glyphWidth
                }

                // Update currentLineMaxHeight
                if (glyphHeight > currentLineMaxHeight) {
                    currentLineMaxHeight = glyphHeight
                }

                // Do the placement
                glyph.atlasLocation = Point(currentX, currentY)

                currentX += glyphWidth
            }

            // Return the dimension and match it's requirement of being at least (1, 1)
            return Dimension(max(1, maxWidth), max(1, currentY + currentLineMaxHeight))
        }
    }

}
