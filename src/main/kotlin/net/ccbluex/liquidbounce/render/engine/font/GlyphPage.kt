/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import com.mojang.blaze3d.GpuFormat
import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2s
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.GlyphMetrics
import java.awt.font.LineMetrics
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Contains information about the placement of characters in a bitmap
 * and how they are rendered
 */
@JvmRecord
data class GlyphRenderInfo(
    /**
     * Which Unicode codepoint does this glyph represent?
     */
    val codepoint: Int,
    /**
     * The location of the Glyph on the sprite, may be null if the glyph is a whitespace
     */
    val atlasLocation: GlyphAtlasLocation?,
    /**
     * The bounds of the rendered glyph. Used for rendering.
     */
    val glyphBounds: BoundingBox2f,
    val layoutInfo: GlyphLayoutInfo
)

class GlyphAtlasLocation(val pixelBoundingBox: BoundingBox2f, atlasDimensions: Dimension) {
    val uvCoordinatesOnTexture: BoundingBox2s
    val atlasWidth: Float
    val atlasHeight: Float

    init {
        val atlasWidth = atlasDimensions.width.toFloat()
        val atlasHeight = atlasDimensions.height.toFloat()

        this.uvCoordinatesOnTexture = BoundingBox2s(
            pixelBoundingBox.xMin / atlasWidth,
            pixelBoundingBox.yMin / atlasHeight,
            pixelBoundingBox.xMax / atlasWidth,
            pixelBoundingBox.yMax / atlasHeight,
        )

        this.atlasWidth = pixelBoundingBox.xMax - pixelBoundingBox.xMin
        this.atlasHeight = pixelBoundingBox.yMax - pixelBoundingBox.yMin
    }
}

@JvmRecord
data class GlyphLayoutInfo(val useHorizontalBaseline: Boolean, val advanceX: Float, val advanceY: Float)

abstract class GlyphPage {
    abstract val texture: GlyphAtlasTexture

    companion object {
        /**
         * The max width and height a texture can have.
         *
         * *Only* request this field's value from a thread with an OpenGL context
         */
        @JvmStatic
        protected val maxTextureSize = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            // As specified in the OpenGL reference, GL_MAX_TEXTURE_SIZE must be at least 1024.
            // If it is less than that, an error occurred, the 1024 is just a failsafe.
            max(gpuDevice.deviceInfo.limits().maxTextureSizeForFormat(GpuFormat.R8_UNORM), 1024)
        }

        @JvmStatic
        protected val fontRendererContext = FontRenderContext(AffineTransform(), true, true)

        /** Java2D's native font scaler is shared by static and dynamic atlas generation. */
        @JvmField
        internal val fontRasterizationLock = Any()

        protected const val DEFAULT_PADDING: Int = 1

        /**
         * Used for the Font Atlas generation
         */
        class CharacterGenerationInfo(
            val fontGlyph: FontGlyph,
            val glyphMetrics: GlyphMetrics,
            val lineMetrics: LineMetrics
        ) {
            lateinit var atlasLocation: Point

            private val bounds = glyphMetrics.bounds2D
            val pixelXMin = floor(bounds.minX).toInt()
            val pixelYMin = floor(bounds.minY).toInt()
            val pixelXMax = ceil(bounds.maxX).toInt()
            val pixelYMax = ceil(bounds.maxY).toInt()
            val pixelWidth = pixelXMax - pixelXMin
            val pixelHeight = pixelYMax - pixelYMin

            /**
             * The space the character will take up in the atlas (character size + padding)
             */
            val atlasDimension: Dimension
                get() = Dimension(
                    pixelWidth + DEFAULT_PADDING * 2,
                    pixelHeight + DEFAULT_PADDING * 2
                )
        }

        /**
         * Initializes the static values for glyph pages. Has to be called from a thread with an OpenGL context.
         */
        fun init() {
            maxTextureSize.value
        }

        @JvmStatic
        protected fun createBufferedImageWithDimensions(atlasDimensions: Dimension) =
            BufferedImage(atlasDimensions.width, atlasDimensions.height, BufferedImage.TYPE_BYTE_GRAY)

        @JvmStatic
        protected fun renderGlyphs(
            atlas: BufferedImage,
            glyphsToRender: Iterable<CharacterGenerationInfo>,
        ) {
            // Allocate the atlas texture
            val atlasGraphics = atlas.createGraphics()

            // Enable font antialiasing
            atlasGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            val glyphsByFont = glyphsToRender.groupBy { it.fontGlyph.font }

            for ((font, glyphs) in glyphsByFont) {
                // Make the graphics object use the specified font
                atlasGraphics.font = font.awtFont

                // Draw glyphs onto the image
                for (characterInfo in glyphs) {
                    renderGlyphOnAtlas(characterInfo, atlasGraphics)
                }

            }

            atlasGraphics.dispose()
        }

        private fun renderGlyphOnAtlas(characterInfo: CharacterGenerationInfo, atlasGraphics: Graphics2D) {
            // Whitespaces are not placed, so they are not rendered
            if (characterInfo.glyphMetrics.isWhitespace) {
                return
            }

            atlasGraphics.paint = Color(0, 0, 0, 0)
            atlasGraphics.composite = AlphaComposite.Clear
            atlasGraphics.fillRect(
                characterInfo.atlasLocation.x,
                characterInfo.atlasLocation.y,
                characterInfo.atlasDimension.width,
                characterInfo.atlasDimension.height
            )
            atlasGraphics.paint = Color.white
            atlasGraphics.composite = AlphaComposite.SrcOver

            // Draw the character to the atlas, offset by start of the character + a pixel padding
            synchronized(fontRasterizationLock) {
                atlasGraphics.drawString(
                    Character.toString(characterInfo.fontGlyph.codepoint),
                    characterInfo.atlasLocation.x - characterInfo.pixelXMin + DEFAULT_PADDING,
                    characterInfo.atlasLocation.y - characterInfo.pixelYMin + DEFAULT_PADDING
                )
            }
        }

        @JvmStatic
        protected fun createGlyphFromGenerationInfo(
            it: CharacterGenerationInfo,
            atlasDimensions: Dimension
        ): GlyphRenderInfo {
            val atlasLocation = if (!it.glyphMetrics.isWhitespace) {
                val x = it.atlasLocation.x.toFloat() + DEFAULT_PADDING
                val y = it.atlasLocation.y.toFloat() + DEFAULT_PADDING

                val boundingBox = BoundingBox2f(
                    x,
                    y,
                    x + it.pixelWidth,
                    y + it.pixelHeight
                )

                GlyphAtlasLocation(boundingBox, atlasDimensions)
            } else {
                null
            }

            return GlyphRenderInfo(
                it.fontGlyph.codepoint,
                atlasLocation = atlasLocation,
                glyphBounds = BoundingBox2f(
                    it.pixelXMin.toFloat(),
                    it.pixelYMin.toFloat(),
                    it.pixelXMax.toFloat(),
                    it.pixelYMax.toFloat(),
                ),
                layoutInfo = GlyphLayoutInfo(
                    useHorizontalBaseline = false, // TODO Find this out
                    advanceX = it.glyphMetrics.advanceX,
                    advanceY = it.glyphMetrics.advanceY
                )
            )
        }

        @JvmStatic
        protected fun createCharacterCreationInfo(it: FontGlyph): CharacterGenerationInfo? =
            synchronized(fontRasterizationLock) {
                val font = it.font.awtFont

                if (!font.canDisplay(it.codepoint)) {
                    return@synchronized null
                }

                val charString = Character.toString(it.codepoint)
                val glyphVector = font.createGlyphVector(fontRendererContext, charString)

                val lineMetrics = font.getLineMetrics(charString, fontRendererContext)
                val glyph = glyphVector.getGlyphMetrics(0)

                CharacterGenerationInfo(it, glyph, lineMetrics)
            }
    }
}

@JvmRecord
data class FontGlyph(val codepoint: Int, val font: FontId)
