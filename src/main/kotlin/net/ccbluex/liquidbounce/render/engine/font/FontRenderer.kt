/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.render.AbstractFontRenderer
import net.ccbluex.liquidbounce.render.engine.*
import net.ccbluex.liquidbounce.render.engine.memory.*
import net.ccbluex.liquidbounce.render.shaders.ColoredPrimitiveShader
import net.ccbluex.liquidbounce.render.shaders.TexturedPrimitiveShader
import net.ccbluex.liquidbounce.utils.render.quad
import java.awt.Font
import java.util.*
import kotlin.math.max
import kotlin.random.Random

data class RenderedGlyph(
    val style: Int,
    val glyph: Glyph,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val z: Float,
    val color: Color4b
)

data class RenderedLine(val p1: Vec3, val p2: Vec3, val color: Color4b)

private class FontRendererCache {
    val renderedGlyphs: ArrayList<RenderedGlyph> = ArrayList(100)
    val lines: ArrayList<RenderedLine> = ArrayList()
}

class FontRenderer(
    /**
     * Glyph pages for the style of the font. If an element is null, fall back to `[0]`
     *
     * [Font.PLAIN] -> 0 (Must not be null)
     *
     * [Font.BOLD] -> 1 (Can be null)
     *
     * [Font.ITALIC] -> 2 (Can be null)
     *
     * [Font.BOLD] | [Font.ITALIC] -> 3 (Can be null)
     */
    private val glyphPages: Array<GlyphPage?>,
    override val size: Float
) : AbstractFontRenderer() {

    /**
     * The cache the drawn structures are stored in until they are packed into a [RenderTask]
     */
    private val cache = FontRendererCache()
    override val height: Float
    val ascent: Float

    init {
        if (this.glyphPages[0] == null) {
            throw IllegalArgumentException("glyphPages[0] must not be null.")
        }

        this.height = glyphPages.maxByOrNull { it?.height ?: 0.0f }!!.height
        this.ascent = glyphPages.maxByOrNull { it?.ascent ?: 0.0f }!!.ascent
    }

    companion object {
        /**
         * Contains the chars for the `§k` formatting
         */
        val RANDOM_CHARS = "1234567890abcdefghijklmnopqrstuvwxyz~!@#\$%^&*()-=_+{}[]".toCharArray()

        @JvmStatic
        val hexColors: Array<Color4b> = Array(16) { i ->
            val baseColor = (i shr 3 and 1) * 85
            val red = (i shr 2 and 1) * 170 + baseColor + if (i == 6) 85 else 0
            val green = (i shr 1 and 1) * 170 + baseColor
            val blue = (i and 1) * 170 + baseColor

            Color4b(red, green, blue, 255)
        }

        /**
         * Creates a FontRenderer that can render every ASCII character.
         *
         * It generates glyph pages for all possible styles.
         */
        fun createFontRenderer(font: Font): FontRenderer {
            return FontRenderer(
                Array(4) { style -> GlyphPage.create('\u0000'..'\u00FF', font.deriveFont(style)) },
                font.size.toFloat()
            )
        }

        /**
         * Creates a FontRenderer that can render every ASCII character.
         *
         * It generates glyph pages for all possible styles.
         */
        fun createFontRendererWithStyles(font: Font, vararg styles: Int): FontRenderer {
            return FontRenderer(
                Array(4) { style ->
                    if (style != 0 && !styles.contains(style)) {
                        null
                    } else GlyphPage.create(
                        '\u0000'..'\u00FF',
                        font.deriveFont(style)
                    )
                },
                font.size.toFloat()
            )
        }

        private fun getColorIndex(type: Char): Int {
            return when (type) {
                in '0'..'9' -> type - '0'
                in 'a'..'f' -> type - 'a' + 10
                in 'k'..'o' -> type - 'k' + 16
                'r' -> 21
                else -> -1
            }
        }

    }

    override fun begin() {
        if (this.cache.renderedGlyphs.isNotEmpty() || this.cache.lines.isNotEmpty()) {
            this.commit()

            throw IllegalStateException("Can't begin a build a new batch when there are pending operations.")
        }
    }

    override fun draw(
        text: String,
        x0: Float,
        y0: Float,
        defaultColor: Color4b,
        shadow: Boolean,
        z: Float,
        scale: Float
    ): Float {
        var len = 0.0f
        // Create a common seed for rendering random fonts
        val seed = Random.nextLong()

        if (shadow) {
            len = drawInternal(text, x0 + 2.0f * scale, y0 + 2.0f * scale, Color4b(0, 0, 0, 150), true, seed, z, scale)
        }

        return max(len, drawInternal(text, x0, y0, defaultColor, false, seed, z, scale))
    }

    /**
     * Draws a string with minecraft font markup to this object.
     *
     * @param defaultColor The color all chars are drawn when no style is specified from Minecraft formatting
     * @param shadow Disables changing of colors, useful for shadows
     * @param obfuscatedSeed Used to sync the obfuscated strings of text with and without shadow.
     * @return The resulting x value
     */
    private fun drawInternal(
        text: String,
        x0: Float,
        y0: Float,
        defaultColor: Color4b,
        shadow: Boolean,
        obfuscatedSeed: Long,
        z: Float,
        scale: Float
    ): Float {
        if (text.isEmpty()) {
            return x0
        }

        // Used for obfuscated strings
        val obfuscatedRandom = Random(obfuscatedSeed)

        var x = x0
        var y = y0 + this.ascent * scale

        var strikeThroughStart = 0.0f
        var underlineStart = 0.0f

        // Was the last read character a §?
        var wasParagraph = false

        // Which style are we rendering atm?
        var style = 0

        // Are we supposed to render random characters?
        var obfuscated = false

        var underline = false
        var strikeThrough = false

        var currentColor = defaultColor

        val defaultStyle = this.glyphPages[0]!!

        for (codepoint in text.chars()) {
            val char = codepoint.toChar()

            // Don't draw paragraph characters, but remember that we found them
            if (char == '§') {
                wasParagraph = true
                continue
            }

            if (wasParagraph) {
                wasParagraph = false

                var shouldContinue = false

                when (val colorIndex = getColorIndex(char)) {
                    in 0..15 -> {
                        // Don't change the color of a shadow pls
                        if (!shadow) {
                            currentColor = hexColors[colorIndex]
                        }

                        if (underline) {
                            drawLine(underlineStart, x, y, z, currentColor, false)
                        }
                        if (strikeThrough) {
                            drawLine(strikeThroughStart, x, y, z, currentColor, true)
                        }

                        style = 0
                        obfuscated = false
                        underline = false
                        strikeThrough = false
                    }
                    16 -> obfuscated = true
                    17 -> style = style or Font.BOLD
                    18 -> {
                        if (!underline) {
                            strikeThroughStart = x
                            strikeThrough = true
                        }
                    }
                    19 -> {
                        if (!underline) {
                            underlineStart = x
                            underline = true
                        }
                    }
                    20 -> style = style or Font.ITALIC
                    21 -> {
                        currentColor = defaultColor

                        if (underline) {
                            drawLine(underlineStart, x, y, z, currentColor, false)
                        }
                        if (strikeThrough) {
                            drawLine(strikeThroughStart, x, y, z, currentColor, true)
                        }

                        style = 0
                        obfuscated = false
                        underline = false
                        strikeThrough = false
                    }
                    else -> shouldContinue = true
                }

                if (!shouldContinue) {
                    continue
                }
            }

            val glyphPage = glyphPages[style] ?: defaultStyle

            // Decide which char we are *really* rendering
            val currentChar = if (obfuscated) RANDOM_CHARS.random(obfuscatedRandom) else char

            val glyph = glyphPage.glyphs[currentChar] ?: glyphPage.glyphs['?']!!

            // We don't need to render whitespaces.
            if (!glyph.isWhitespace) {
                this.cache.renderedGlyphs.add(
                    RenderedGlyph(
                        style,
                        glyph,
                        x + glyph.glyphBounds.xMin * scale,
                        y + glyph.glyphBounds.yMin * scale,
                        x + (glyph.glyphBounds.xMin + glyph.atlasWidth) * scale,
                        y + (glyph.glyphBounds.yMin + glyph.atlasHeight) * scale,
                        z,
                        currentColor
                    )
                )
            }

            x += glyph.advanceX * scale
            y += glyph.advanceY * scale
        }

        if (underline) {
            drawLine(underlineStart, x, y, z, currentColor, false)
        }
        if (strikeThrough) {
            drawLine(strikeThroughStart, x, y, z, currentColor, true)
        }

        return x
    }

    override fun getStringWidth(
        text: String,
        shadow: Boolean
    ): Float {
        if (text.isEmpty()) {
            return 0.0f
        }

        var x = 0.0f

        // Was the last read character a §?
        var wasParagraph = false

        // Which style are we rendering atm?
        var style = 0

        // Are we supposed to render random characters?
        var obfuscated = false

        val defaultStyle = this.glyphPages[0]!!

        for (codepoint in text.chars()) {
            val char = codepoint.toChar()

            // Don't draw paragraph characters, but remember that we found them
            if (char == '§') {
                wasParagraph = true
                continue
            }

            if (wasParagraph) {
                wasParagraph = false

                var shouldContinue = false

                when (val colorIndex = getColorIndex(char)) {
                    in 0..15, 21 -> {
                        style = 0
                        obfuscated = false
                    }
                    16 -> obfuscated = true
                    17 -> style = style or Font.BOLD
                    20 -> style = style or Font.ITALIC
                    else -> shouldContinue = true
                }

                if (!shouldContinue) {
                    continue
                }
            }

            val glyphPage = glyphPages[style] ?: defaultStyle

            // Decide which char we are *really* rendering
            val currentChar = if (obfuscated) '_' else char

            val glyph = glyphPage.glyphs[currentChar] ?: glyphPage.glyphs['?']!!

            x += glyph.advanceX
        }

        return if (shadow) {
            x + 2.0f
        } else {
            x
        }
    }

    private fun drawLine(
        x0: Float,
        x: Float,
        y: Float,
        z: Float,
        color: Color4b,
        through: Boolean
    ) {
        if (through) {
            this.cache.lines.add(
                RenderedLine(
                    Vec3(x0, y - this.height + this.ascent, z),
                    Vec3(x, y - this.height + this.ascent, z),
                    color
                )
            )
        } else {
            this.cache.lines.add(
                RenderedLine(
                    Vec3(x0, y + 1.0f, z),
                    Vec3(x, y + 1.0f, z),
                    color
                )
            )
        }

    }

    override fun commit(): Array<RenderTask> {
        val tasks = ArrayList<RenderTask>(5)
        val renderTasks = this.cache.renderedGlyphs.groupByTo(TreeMap<Int, MutableList<RenderedGlyph>>()) { it.style }

        for ((style, glyphs) in renderTasks) {
            val vertexFormat = PositionColorUVVertexFormat()

            vertexFormat.initBuffer(glyphs.size * 4)

            val indexBuffer = IndexBuffer(glyphs.size * 3 * 2, VertexFormatComponentDataType.GlUnsignedShort)

            for (glyph in glyphs) {
                val color = glyph.color
                val atlasLocation = glyph.glyph.atlasLocation!!

                vertexFormat.quad(
                    indexBuffer,
                    {
                        this.position = Vec3(glyph.x1, glyph.y1, glyph.z)
                        this.color = color
                        this.texturePosition = UV2s(atlasLocation.min.u, atlasLocation.min.v)
                    },
                    {
                        this.position = Vec3(glyph.x1, glyph.y2, glyph.z)
                        this.color = color
                        this.texturePosition = UV2s(atlasLocation.min.u, atlasLocation.max.v)
                    },
                    {
                        this.position = Vec3(glyph.x2, glyph.y2, glyph.z)
                        this.color = color
                        this.texturePosition = UV2s(atlasLocation.max.u, atlasLocation.max.v)
                    },
                    {
                        this.position = Vec3(glyph.x2, glyph.y1, glyph.z)
                        this.color = color
                        this.texturePosition = UV2s(atlasLocation.max.u, atlasLocation.min.v)
                    }
                )
            }

            tasks.add(VertexFormatRenderTask(vertexFormat, PrimitiveType.Triangles, TexturedPrimitiveShader, indexBuffer = indexBuffer, texture = glyphPages[style]!!.texture, state = GlRenderState(texture2d = true, depthTest = false)))
        }

        if (this.cache.lines.isNotEmpty()) {
            val vertexFormat = PositionColorVertexFormat()

            vertexFormat.initBuffer(this.cache.lines.size * 2)

            for (line in this.cache.lines) {
                vertexFormat.putVertex {
                    this.position = line.p1
                    this.color = line.color
                }
                vertexFormat.putVertex {
                    this.position = line.p2
                    this.color = line.color
                }
            }

            tasks.add(VertexFormatRenderTask(vertexFormat, PrimitiveType.Lines, ColoredPrimitiveShader))
        }

        this.cache.lines.clear()
        this.cache.renderedGlyphs.clear()

        return tasks.toTypedArray()
    }

}
