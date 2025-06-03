/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.type.UV2f
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

data class BoundingBox2f(val xMin: Float, val yMin: Float, val xMax: Float, val yMax: Float) {
    constructor(rect: Rectangle2D) : this(
        rect.x.toFloat(),
        rect.y.toFloat(),
        rect.width.toFloat(),
        rect.height.toFloat()
    )

    fun contains(x: Float, y: Float): Boolean {
        return x in xMin..xMax && y in yMin..yMax
    }

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
data class GlyphRenderInfo(
    /**
     * Which char does this glyph represent?
     */
    val char: Char,
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
            BoundingBox2f(
                pixelBoundingBox.xMin / atlasWidth,
                pixelBoundingBox.yMin / atlasHeight,
                pixelBoundingBox.xMax / atlasWidth,
                pixelBoundingBox.yMax / atlasHeight
            )
        )

        this.atlasWidth = pixelBoundingBox.xMax - pixelBoundingBox.xMin
        this.atlasHeight = pixelBoundingBox.yMax - pixelBoundingBox.yMin
    }
}
data class GlyphLayoutInfo(val useHorizontalBaseline: Boolean, val advanceX: Float, val advanceY: Float)

abstract class GlyphPage {
    abstract val texture: NativeImageBackedTexture

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
            max(GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE), 1024)
        }

        @JvmStatic
        protected val fontRendererContext = FontRenderContext(AffineTransform(), true, true)

        @JvmStatic
        protected val DEFAULT_PADDING: Int = 1


        /**
         * Used for the Font Atlas generation
         */
        class CharacterGenerationInfo(
            val fontGlyph: FontGlyph,
            val glyphMetrics: GlyphMetrics,
            val lineMetrics: LineMetrics
        ) {
            lateinit var atlasLocation: Point

            /**
             * The space the character will take up in the atlas (character size + padding)
             */
            val atlasDimension: Dimension
                get() = Dimension(
                    ceil(glyphMetrics.bounds2D.width).toInt() + 2,
                    ceil(glyphMetrics.bounds2D.height).toInt() + 2
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
            BufferedImage(atlasDimensions.width, atlasDimensions.height, BufferedImage.TYPE_INT_ARGB)

        @JvmStatic
        protected fun renderGlyphs(
            atlas: BufferedImage,
            glyphsToRender: List<CharacterGenerationInfo>
        ) {
            // Allocate the atlas texture
            val atlasGraphics = atlas.createGraphics()

            // Enable font anti aliasing
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
            atlasGraphics.composite = AlphaComposite.getInstance(AlphaComposite.CLEAR)
            atlasGraphics.fillRect(
                characterInfo.atlasLocation.x,
                characterInfo.atlasLocation.y,
                characterInfo.atlasDimension.width,
                characterInfo.atlasDimension.height
            )
            atlasGraphics.paint = Color.white
            atlasGraphics.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)

            // Draw the character to the atlas, offset by start of the character + a pixel padding
            atlasGraphics.drawString(
                characterInfo.fontGlyph.codepoint.toString(),
                characterInfo.atlasLocation.x - characterInfo.glyphMetrics.bounds2D.x.toInt() + 1,
                characterInfo.atlasLocation.y - characterInfo.glyphMetrics.bounds2D.y.toInt() + 1
            )
        }

        @JvmStatic
        protected fun createGlyphFromGenerationInfo(
            it: CharacterGenerationInfo,
            atlasDimensions: Dimension
        ): GlyphRenderInfo {
            val atlasLocation = if (!it.glyphMetrics.isWhitespace) {
                val x = it.atlasLocation.x.toFloat()
                val y = it.atlasLocation.y.toFloat()

                val boundingBox = BoundingBox2f(
                    x,
                    y,
                    (x + ceil(it.glyphMetrics.bounds2D.width.toFloat()) + DEFAULT_PADDING),
                    (y + ceil(it.glyphMetrics.bounds2D.height.toFloat()) + DEFAULT_PADDING)
                )

                GlyphAtlasLocation(boundingBox, atlasDimensions)
            } else {
                null
            }

            return GlyphRenderInfo(
                it.fontGlyph.codepoint,
                atlasLocation = atlasLocation,
                glyphBounds = BoundingBox2f(it.glyphMetrics.bounds2D),
                layoutInfo = GlyphLayoutInfo(
                    useHorizontalBaseline = false, // TODO Find this out
                    advanceX = it.glyphMetrics.advanceX,
                    advanceY = it.glyphMetrics.advanceY
                )
            )
        }

        @JvmStatic
        protected fun createCharacterCreationInfo(it: FontGlyph): CharacterGenerationInfo? {
            val font = it.font.awtFont

            if (!font.canDisplay(it.codepoint)) {
                return null
            }

            val charString = it.codepoint.toString()
            val glyphVector = font.createGlyphVector(fontRendererContext, charString)

            val lineMetrics = font.getLineMetrics(charString, fontRendererContext)
            val glyph = glyphVector.getGlyphMetrics(0)

            return CharacterGenerationInfo(it, glyph, lineMetrics)
        }
    }
}

data class FontGlyph(val codepoint: Char, val font: FontManager.FontId)

internal fun BufferedImage.toNativeImage(): NativeImage {
    val nativeImage = NativeImage(NativeImage.Format.RGBA, this.width, this.height, false)

    // Fuck Minecraft native image
    for (x in 0 until this.width) {
        for (y in 0 until this.height) {
            nativeImage.setColorArgb(x, y, this.getRGB(x, y))
        }
    }

    return nativeImage
}
