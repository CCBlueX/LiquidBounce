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

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleNameProtect
import net.ccbluex.liquidbounce.features.module.modules.misc.sanitizeWithNameProtect
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.engine.font.processor.LegacyTextProcessor
import net.ccbluex.liquidbounce.render.engine.font.processor.MinecraftTextProcessor
import net.ccbluex.liquidbounce.render.engine.font.processor.TextProcessor
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f
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
    val glyphPages: Array<FontGlyphPageManager?>,
    override val size: Float
) : AbstractFontRenderer<TextProcessor.ProcessedText>() {

    private val cache = FontRendererCache()
    override val height: Float
    val ascent: Float

    init {
        require(this.glyphPages[0] != null) {
            "glyphPages[0] must not be null."
        }

        this.height = glyphPages.maxByOrNull { it?.height ?: 0.0f }!!.height
        this.ascent = glyphPages.maxByOrNull { it?.ascent ?: 0.0f }!!.ascent
    }

    private val defaultStyle = glyphPages.first { it != null } ?: error("No valid glyph page found.")

    override fun begin() {
        if (this.cache.renderedGlyphs.isNotEmpty() || this.cache.lines.isNotEmpty()) {
//            this.commit()

            error("Can't begin a build a new batch when there are pending operations.")
        }
    }

    override fun process(text: String, defaultColor: Color4b): TextProcessor.ProcessedText {
        return LegacyTextProcessor(ModuleNameProtect.replace(text), defaultColor, Random.nextLong()).process()
    }

    override fun process(text: Text, defaultColor: Color4b): TextProcessor.ProcessedText {
        return MinecraftTextProcessor(text.sanitizeWithNameProtect(), defaultColor, Random.nextLong()).process()
    }

    override fun draw(
        text: TextProcessor.ProcessedText,
        x0: Float,
        y0: Float,
        shadow: Boolean,
        z: Float,
        scale: Float
    ): Float {
        var len = 0.0f

        if (shadow) {
            len = drawInternal(
                text,
                pos = Vector3f(x0 + 2.0f * scale, y0 + 2.0f * scale, z),
                scale,
                overrideColor = Color4b(0, 0, 0, 150)
            )
        }

        return max(len, drawInternal(text, Vector3f(x0, y0, z * 2.0F), scale))
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
        text: TextProcessor.ProcessedText,
        pos: Vector3f,
        scale: Float,
        overrideColor: Color4b? = null
    ): Float {
        if (text.chars.isEmpty()) {
            return pos.x
        }

        val underlineStack = ArrayList<IntRange>(text.underlines.asReversed())
        val strikethroughStack = ArrayList<IntRange>(text.strikeThroughs.asReversed())

        var x = pos.x
        var y = pos.y + this.ascent * scale

        var strikeThroughStartX: Float? = null
        var underlineStartX: Float? = null

        // Which style are we rendering atm?
        var style = 0

        text.chars.forEachIndexed { charIdx, processedChar ->
            val glyphPage = glyphPages[style] ?: defaultStyle

            val glyph = glyphPage.staticPage.glyphs[processedChar.char] ?: glyphPage.staticPage.fallbackGlyph
            val color = overrideColor ?: processedChar.color

            if (underlineStack.lastOrNull()?.start == charIdx) {
                underlineStartX = x
            }
            if (strikethroughStack.lastOrNull()?.start == charIdx) {
                strikeThroughStartX = x
            }

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
                        pos.z,
                        color
                    )
                )
            }

            val advanceX =
                if (!processedChar.obfuscated) glyph.advanceX else glyphPage.staticPage.glyphs['_']!!.advanceX

            x += advanceX * scale
            y += glyph.advanceY * scale

            if (underlineStack.lastOrNull()?.endInclusive == charIdx) {
                underlineStack.removeLast()

                drawLine(underlineStartX!!, x, y, pos.z, color, false)
            }
            if (strikethroughStack.lastOrNull()?.endInclusive == charIdx) {
                strikethroughStack.removeLast()

                drawLine(strikeThroughStartX!!, x, y, pos.z, color, true)
            }
        }

        return x
    }

    override fun getStringWidth(
        text: TextProcessor.ProcessedText,
        shadow: Boolean
    ): Float {
        if (text.chars.isEmpty()) {
            return 0.0f
        }

        var x = 0.0f

        for (processedChar in text.chars) {
            val glyphPage = glyphPages[processedChar.font] ?: defaultStyle

            val glyph = glyphPage.staticPage.glyphs[processedChar.char] ?: glyphPage.staticPage.fallbackGlyph

            val advanceX =
                if (!processedChar.obfuscated) glyph.advanceX else glyphPage.staticPage.glyphs['_']!!.advanceX

            x += advanceX
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

    override fun commit(
        env: RenderEnvironment,
        buffers: FontRendererBuffers,
    ) {
        val renderTasks = this.cache.renderedGlyphs.groupByTo(TreeMap<Int, MutableList<RenderedGlyph>>()) { it.style }

        for ((style, glyphs) in renderTasks) {
            val textBuilder = buffers.textBuffers[style]

            for (glyph in glyphs) {
                val color = glyph.color
                val atlasLocation = glyph.glyph.atlasLocation!!

                textBuilder.drawQuad(
                    env,
                    Vec3d(glyph.x1.toDouble(), glyph.y1.toDouble(), glyph.z.toDouble()),
                    atlasLocation.min,
                    Vec3d(glyph.x2.toDouble(), glyph.y2.toDouble(), glyph.z.toDouble()),
                    atlasLocation.max,
                    color
                )
            }
        }

        if (this.cache.lines.isNotEmpty()) {
            for (line in this.cache.lines) {
                buffers.lineBufferBuilder.drawLine(env, line.p1, line.p2, line.color)
            }
        }

        this.cache.lines.clear()
        this.cache.renderedGlyphs.clear()
    }

}


class FontRendererBuffers {
    companion object {
        private val TEXT_TESSELATORS = Array(5) { Tessellator(0xA00000) }
    }

    val textBuffers = Array(4) {
        RenderBufferBuilder(VertexFormat.DrawMode.QUADS, VertexInputType.PosTexColor, TEXT_TESSELATORS[it + 1])
    }
    val lineBufferBuilder =
        RenderBufferBuilder(VertexFormat.DrawMode.DEBUG_LINES, VertexInputType.PosColor, TEXT_TESSELATORS[0])

    fun draw(renderer: FontRenderer) {
        this.textBuffers.forEachIndexed { style, bufferBuilder ->
            val tex = renderer.glyphPages[style]!!.staticPage.texture

            RenderSystem.bindTexture(tex.glId)

            RenderSystem.setShaderTexture(0, tex.glId)

            bufferBuilder.draw()
        }

        this.lineBufferBuilder.draw()
    }

    fun reset() {
        this.textBuffers.forEachIndexed { style, bufferBuilder ->
            bufferBuilder.reset()
        }

        this.lineBufferBuilder.reset()
    }
}

