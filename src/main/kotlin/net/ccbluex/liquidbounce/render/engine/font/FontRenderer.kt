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

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.FontManager.DEFAULT_FONT_SIZE
import net.ccbluex.liquidbounce.render.engine.font.processor.MinecraftTextProcessor
import net.ccbluex.liquidbounce.render.engine.font.processor.TextProcessor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.client.asText
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f
import java.awt.Font
import kotlin.math.max
import kotlin.random.Random

data class RenderedGlyph(
    val style: Int,
    val glyph: GlyphDescriptor,
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
    val font: FontManager.FontFace,
    val glyphManager: FontGlyphPageManager,
    override val size: Float = DEFAULT_FONT_SIZE
) : AbstractFontRenderer<TextProcessor.ProcessedText>() {

    private val cache = FontRendererCache()
    override val height: Float = font.styles.firstNotNullOf { it?.height }
    val ascent: Float = font.styles.firstNotNullOf { it?.ascent }


    override fun begin() {
        if (this.cache.renderedGlyphs.isNotEmpty() || this.cache.lines.isNotEmpty()) {
//            this.commit()

            error("Can't begin a build a new batch when there are pending operations.")
        }
    }

    override fun process(text: String, defaultColor: Color4b): TextProcessor.ProcessedText {
        return process(text.asText(), defaultColor)
    }

    override fun process(text: Text, defaultColor: Color4b): TextProcessor.ProcessedText {
        return MinecraftTextProcessor(text.sanitizeForeignInput(), defaultColor, Random.nextLong()).process()
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

        val fallbackGlyph = this.glyphManager.getFallbackGlyph(this.font)

        text.chars.forEachIndexed { charIdx, processedChar ->
            val glyph = this.glyphManager.requestGlyph(this.font, processedChar.font, processedChar.char)
                ?: fallbackGlyph
            val color = overrideColor ?: processedChar.color

            if (underlineStack.lastOrNull()?.start == charIdx) {
                underlineStartX = x
            }
            if (strikethroughStack.lastOrNull()?.start == charIdx) {
                strikeThroughStartX = x
            }

            // We don't need to render whitespaces.
            val renderInfo = glyph.renderInfo
            val atlasLocation = renderInfo.atlasLocation

            // We don't need to render whitespaces.
            if (atlasLocation != null) {
                val renderedGlyph = RenderedGlyph(
                    processedChar.font,
                    glyph,
                    x + renderInfo.glyphBounds.xMin * scale,
                    y + renderInfo.glyphBounds.yMin * scale,
                    x + (renderInfo.glyphBounds.xMin + atlasLocation.atlasWidth) * scale,
                    y + (renderInfo.glyphBounds.yMin + atlasLocation.atlasHeight) * scale,
                    pos.z,
                    color
                )

                this.cache.renderedGlyphs.add(renderedGlyph)
            }

            val layoutInfo =
                if (!processedChar.obfuscated) renderInfo.layoutInfo else fallbackGlyph.renderInfo.layoutInfo

            x += layoutInfo.advanceX * scale
            y += layoutInfo.advanceY * scale

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

        val fallbackGlyph = this.glyphManager.getFallbackGlyph(this.font)

        for (processedChar in text.chars) {
            val glyph = this.glyphManager.requestGlyph(this.font, processedChar.font, processedChar.char)
                ?: fallbackGlyph

            val layoutInfo =
                if (!processedChar.obfuscated) glyph.renderInfo.layoutInfo else fallbackGlyph.renderInfo.layoutInfo

            x += layoutInfo.advanceX
        }

        return if (shadow) {
            x + 2.0f
        } else {
            x
        }
    }

    @Suppress("LongParameterList")
    private fun drawLine(
        x0: Float,
        x1: Float,
        y: Float,
        z: Float,
        color: Color4b,
        through: Boolean
    ) {
        if (through) {
            this.cache.lines.add(
                RenderedLine(
                    Vec3(x0, y - this.height + this.ascent, z),
                    Vec3(x1, y - this.height + this.ascent, z),
                    color
                )
            )
        } else {
            this.cache.lines.add(
                RenderedLine(
                    Vec3(x0, y + 1.0f, z),
                    Vec3(x1, y + 1.0f, z),
                    color
                )
            )
        }

    }

    override fun commit(
        env: RenderEnvironment,
        buffers: FontRendererBuffers,
    ) {
        this.cache.renderedGlyphs.forEach { renderedGlyph ->
            val glyphDescriptor = renderedGlyph.glyph
            val renderBuffer = buffers.getTextBufferForGlyphPage(glyphDescriptor.page)

            val color = renderedGlyph.color
            val atlasLocation = glyphDescriptor.renderInfo.atlasLocation!!

            renderBuffer.drawQuad(
                env,
                Vec3d(renderedGlyph.x1.toDouble(), renderedGlyph.y1.toDouble(), renderedGlyph.z.toDouble()),
                atlasLocation.uvCoordinatesOnTexture.min,
                Vec3d(renderedGlyph.x2.toDouble(), renderedGlyph.y2.toDouble(), renderedGlyph.z.toDouble()),
                atlasLocation.uvCoordinatesOnTexture.max,
                color
            )
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
        private var currentTessellatorIndex = 1

        private val textTesselatorMap = HashMap<GlyphPage, Tessellator>()

        fun getTesselatorForGlyphPage(glyphPage: GlyphPage): Tessellator {
            return textTesselatorMap.computeIfAbsent(glyphPage) { TEXT_TESSELATORS[currentTessellatorIndex++] }
        }
    }

    val textBuffers = HashMap<GlyphPage, RenderBufferBuilder<VertexInputType.PosTexColor>>()

    fun getTextBufferForGlyphPage(glyphPage: GlyphPage): RenderBufferBuilder<VertexInputType.PosTexColor> {
        return this.textBuffers.computeIfAbsent(glyphPage) {
            val tessellator = getTesselatorForGlyphPage(glyphPage)

            RenderBufferBuilder(VertexFormat.DrawMode.QUADS, VertexInputType.PosTexColor, tessellator)
        }
    }

    val lineBufferBuilder =
        RenderBufferBuilder(VertexFormat.DrawMode.DEBUG_LINES, VertexInputType.PosColor, TEXT_TESSELATORS[0])

    fun draw() {
        this.textBuffers.forEach { (glyphPage, bufferBuilder) ->
            val tex = glyphPage.texture

            RenderSystem.bindTexture(tex.glId)

            RenderSystem.setShaderTexture(0, tex.glId)

            bufferBuilder.draw()
        }

        this.lineBufferBuilder.draw()
    }

    fun reset() {
        this.textBuffers.forEach { (_, bufferBuilder) ->
            bufferBuilder.reset()
        }

        this.lineBufferBuilder.reset()
    }
}

