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
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.ccbluex.fastutil.Pool
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.FontManager.DEFAULT_FONT_SIZE
import net.ccbluex.liquidbounce.render.engine.font.processor.MinecraftTextProcessor
import net.ccbluex.liquidbounce.render.engine.font.processor.ProcessedText
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.minecraft.client.render.VertexFormat
import net.minecraft.text.Text
import org.joml.Vector3f
import java.awt.Font
import kotlin.math.max

@JvmRecord
private data class RenderedGlyph(
    val style: Int,
    val glyph: GlyphDescriptor,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val z: Float,
    val color: Color4b
)

@JvmRecord
private data class RenderedLine(val p1: Vector3f, val p2: Vector3f, val color: Color4b)

private class FontRendererCache {
    val renderedGlyphs = ArrayList<RenderedGlyph>(100)
    val commitGlyphs = Reference2ReferenceOpenHashMap<GlyphPage, ArrayList<RenderedGlyph>>()
    val renderedGlyphListPool = Pool(
        ::ArrayList,
        ArrayList<RenderedGlyph>::clear,
    )
    val lines = ArrayList<RenderedLine>()
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
) : AbstractFontRenderer<MinecraftTextProcessor.RecyclingProcessedText>() {

    private val cache = FontRendererCache()
    private val positionCache = Vector3f()
    private val underlinesCache = ArrayDeque<IntRange>()
    private val strikethroughCache = ArrayDeque<IntRange>()

    override val height: Float = font.styles.firstNotNullOf { it?.height }

    val ascent: Float = font.styles.firstNotNullOf { it?.ascent }

    private val shadowColor = Color4b(0, 0, 0, 150)

    override fun begin() {
        if (this.cache.renderedGlyphs.isNotEmpty() || this.cache.lines.isNotEmpty()) {
//            this.commit()

            error("Can't begin a build a new batch when there are pending operations.")
        }
    }

    override fun process(text: String, defaultColor: Color4b): MinecraftTextProcessor.RecyclingProcessedText {
        return process(text.asPlainText(), defaultColor)
    }

    override fun process(text: Text, defaultColor: Color4b): MinecraftTextProcessor.RecyclingProcessedText {
        return MinecraftTextProcessor.process(text.sanitizeForeignInput(), defaultColor)
    }

    override fun draw(
        text: MinecraftTextProcessor.RecyclingProcessedText,
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
                pos = positionCache.set(x0 + 2.0f * scale, y0 + 2.0f * scale, z),
                scale,
                overrideColor = shadowColor
            )
        }

        len = max(len, drawInternal(text, positionCache.set(x0, y0, z * 2.0F), scale))

        MinecraftTextProcessor.TEXT_POOL.recycle(text)

        return len
    }

    /**
     * Draws a string with minecraft font markup to this object.
     *
     * @return The resulting x value
     */
    private fun drawInternal(
        text: ProcessedText,
        pos: Vector3f,
        scale: Float,
        overrideColor: Color4b? = null
    ): Float {
        if (text.chars.isEmpty()) {
            return pos.x
        }

        // remove from last
        val underlineStack = underlinesCache.apply {
            clear()
            addAll(text.underlines)
        }
        val strikethroughStack = strikethroughCache.apply {
            clear()
            addAll(text.strikeThroughs)
        }

        var x = pos.x
        var y = pos.y + this.ascent * scale

        var strikeThroughStartX: Float? = null
        var underlineStartX: Float? = null

        val fallbackGlyph = this.glyphManager.getFallbackGlyph(this.font)

        text.chars.forEachIndexed { charIdx, processedChar ->
            val glyph = this.glyphManager.requestGlyph(this.font, processedChar.font, processedChar.char)
                ?: fallbackGlyph
            val color = overrideColor ?: processedChar.color

            if (underlineStack.firstOrNull()?.start == charIdx) {
                underlineStartX = x
            }
            if (strikethroughStack.firstOrNull()?.start == charIdx) {
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

            if (underlineStack.firstOrNull()?.endInclusive == charIdx) {
                underlineStack.removeFirst()

                drawLine(underlineStartX!!, x, y, pos.z, color, false)
            }
            if (strikethroughStack.firstOrNull()?.endInclusive == charIdx) {
                strikethroughStack.removeFirst()

                drawLine(strikeThroughStartX!!, x, y, pos.z, color, true)
            }
        }

        return x
    }

    override fun getStringWidth(
        text: ProcessedText,
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
                    Pools.Vec3f.borrow().set(x0, y - this.height + this.ascent, z),
                    Pools.Vec3f.borrow().set(x1, y - this.height + this.ascent, z),
                    color
                )
            )
        } else {
            this.cache.lines.add(
                RenderedLine(
                    Pools.Vec3f.borrow().set(x0, y + 1.0f, z),
                    Pools.Vec3f.borrow().set(x1, y + 1.0f, z),
                    color
                )
            )
        }

    }

    override fun commit(environment: RenderEnvironment) {
        for (glyph in cache.renderedGlyphs) {
            val glyphPage = glyph.glyph.page
            cache.commitGlyphs.getOrPut(glyphPage) { cache.renderedGlyphListPool.borrow() }.add(glyph)
        }
        cache.renderedGlyphs.clear()

        val vec3f1 = Pools.Vec3f.borrow()
        val vec3f2 = Pools.Vec3f.borrow()
        cache.commitGlyphs.fastIterator().forEach { (glyphPage, renderedGlyphs) ->
            RenderSystem.bindTexture(glyphPage.texture.glId)
            RenderSystem.setShaderTexture(0, glyphPage.texture.glId)

            environment.startBatch()
            for (renderedGlyph in renderedGlyphs) {
                val glyphDescriptor = renderedGlyph.glyph

                val color = renderedGlyph.color
                val atlasLocation = glyphDescriptor.renderInfo.atlasLocation!!

                environment.drawTextureQuad(
                    vec3f1.set(renderedGlyph.x1, renderedGlyph.y1, renderedGlyph.z),
                    atlasLocation.uvCoordinatesOnTexture.min,
                    vec3f2.set(renderedGlyph.x2, renderedGlyph.y2, renderedGlyph.z),
                    atlasLocation.uvCoordinatesOnTexture.max,
                    color.toARGB(),
                )
            }
            environment.commitBatch()
            cache.renderedGlyphListPool.recycle(renderedGlyphs)
        }
        Pools.Vec3f.recycle(vec3f1)
        Pools.Vec3f.recycle(vec3f2)
        cache.commitGlyphs.clear()

        if (cache.lines.isNotEmpty()) {
            environment.startBatch()
            for (line in cache.lines) {
                environment.drawCustomMesh(
                    VertexFormat.DrawMode.DEBUG_LINES,
                    VertexInputType.PosColor,
                ) { matrix ->
                    vertex(matrix, line.p1.x, line.p1.y, line.p1.z).color(line.color.toARGB())
                    vertex(matrix, line.p2.x, line.p2.y, line.p2.z).color(line.color.toARGB())
                }
                Pools.Vec3f.recycle(line.p1)
                Pools.Vec3f.recycle(line.p2)
            }
            environment.commitBatch()
        }

        cache.lines.clear()
    }

}
