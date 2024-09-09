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

abstract class BaseGlpyhPage {
    abstract val texture: NativeImageBackedTexture
    abstract val fallbackGlyph: Glyph

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


        /**
         * Used for the Font Atlas generation
         */
        class CharacterGenerationInfo(val c: Char, val glyphMetrics: GlyphMetrics, val lineMetrics: LineMetrics) {
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
            font: Font,
            glyphsToRender: List<CharacterGenerationInfo>
        ): Pair<BufferedImage, FontMetrics> {
            // Allocate the atlas texture
            val atlasGraphics = atlas.createGraphics()

            // Enable font anti aliasing
            atlasGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            // Make the graphics object use the specified font
            atlasGraphics.font = font

            // Draw glyphs onto the image
            for (characterInfo in glyphsToRender) {
                // Whitespaces are not placed, so they are not rendered
                if (characterInfo.glyphMetrics.isWhitespace) {
                    continue
                }

                atlasGraphics.paint = Color(0, 0, 0, 0)
                atlasGraphics.fillRect(
                    characterInfo.atlasLocation.x,
                    characterInfo.atlasLocation.y,
                    characterInfo.atlasDimension.width,
                    characterInfo.atlasDimension.height
                )
                atlasGraphics.paint = Color.white

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

        @JvmStatic
        protected fun createGlyphFromGenerationInfo(
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

        @JvmStatic
        protected fun createCharacterCreationInfo(
            it: Char,
            font: Font,
        ): CharacterGenerationInfo? {
            if (!font.canDisplay(it)) {
                return null
            }

            val charString = it.toString()
            val glyphVector = font.createGlyphVector(fontRendererContext, charString)

            val lineMetrics = font.getLineMetrics(charString, fontRendererContext)
            val glyph = glyphVector.getGlyphMetrics(0)

            return CharacterGenerationInfo(it, glyph, lineMetrics)
        }
    }

    abstract fun getGlyph(char: Char): Glyph?
}



internal fun BufferedImage.toNativeImage(): NativeImage {
    val nativeImage = NativeImage(NativeImage.Format.RGBA, this.width, this.height, false)

    // Fuck Minecraft native image
    for (x in 0 until this.width) {
        for (y in 0 until this.height) {
            nativeImage.setColor(x, y, this.getRGB(x, y))
        }
    }

    return nativeImage
}
