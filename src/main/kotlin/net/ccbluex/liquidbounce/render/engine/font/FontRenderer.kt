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

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntStack
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.FontManager.DEFAULT_FONT_SIZE
import net.ccbluex.liquidbounce.render.engine.font.processor.MinecraftTextProcessor
import net.ccbluex.liquidbounce.render.engine.font.processor.ProcessedText
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.text.Text
import org.joml.Vector3f
import org.joml.Vector3fc
import java.awt.Font
import kotlin.math.max

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

    // Caches
    private val underlinesIdxStack = IntArrayList()
    private val strikethroughIdxStack = IntArrayList()
    private val positionCache = Vector3f()
    private val charPos1 = Vector3f()
    private val charPos2 = Vector3f()

    override val height: Float = font.styles.firstNotNullOf { it?.height }

    private val ascent: Float = font.styles.firstNotNullOf { it?.ascent }

    private val shadowColor = Color4b(0, 0, 0, 150)

    private fun loadUnderlines(text: ProcessedText): IntStack = underlinesIdxStack.apply {
        clear()
        addAll(text.underlines)
        elements().reverse(0, size)
    }

    private fun loadStrikethroughs(text: ProcessedText): IntStack = strikethroughIdxStack.apply {
        clear()
        addAll(text.strikeThroughs)
        elements().reverse(0, size)
    }

    override fun process(text: Text, defaultColor: Color4b): MinecraftTextProcessor.RecyclingProcessedText {
        return MinecraftTextProcessor.process(text.sanitizeForeignInput(), defaultColor)
    }

    context(environment: GUIRenderEnvironment)
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
    context(environment: GUIRenderEnvironment)
    @Suppress("CognitiveComplexMethod")
    private fun drawInternal(
        text: ProcessedText,
        pos: Vector3fc,
        scale: Float,
        overrideColor: Color4b? = null
    ): Float {
        if (text.chars.isEmpty()) {
            return pos.x()
        }

        val underlineStack = loadUnderlines(text)
        val strikethroughStack = loadStrikethroughs(text)

        var x = pos.x()
        var y = pos.y() + this.ascent * scale
        val z = pos.z()

        var strikeThroughStartX: Float? = null
        var underlineStartX: Float? = null

        val fallbackGlyph = this.glyphManager.getFallbackGlyph(this.font)

        text.chars.forEachIndexed { charIdx, processedChar ->
            val glyph = this.glyphManager.requestGlyph(this.font, processedChar.font, processedChar.char)
                ?: fallbackGlyph
            val color = overrideColor ?: processedChar.color

            if (!underlineStack.isEmpty && underlineStack.topInt() == charIdx) {
                underlineStack.popInt()
                underlineStartX = x
            }
            if (!strikethroughStack.isEmpty && strikethroughStack.topInt() == charIdx) {
                strikethroughStack.popInt()
                strikeThroughStartX = x
            }

            drawChar(glyph, x, y, z, scale, color)

            val layoutInfo =
                if (!processedChar.obfuscated) glyph.renderInfo.layoutInfo else fallbackGlyph.renderInfo.layoutInfo

            x += layoutInfo.advanceX * scale
            y += layoutInfo.advanceY * scale

            if (!underlineStack.isEmpty && underlineStack.topInt() == charIdx) {
                underlineStack.popInt()
                drawLine(underlineStartX!!, x, y, z, color, false)
            }

            if (!strikethroughStack.isEmpty && strikethroughStack.topInt() == charIdx) {
                strikethroughStack.popInt()
                drawLine(strikeThroughStartX!!, x, y, z, color, true)
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

    context(environment: GUIRenderEnvironment)
    private fun drawLine(
        x0: Float,
        x1: Float,
        y: Float,
        z: Float,
        color: Color4b,
        through: Boolean
    ) {
        val y = if (through) y - this.height + this.ascent else y + 1f
        environment.drawCustomMesh(ClientRenderPipelines.Lines) { matrix ->
            vertex(matrix, x0, y, z).color(color)
            vertex(matrix, x1, y, z).color(color)
        }
    }

    context(environment: GUIRenderEnvironment)
    private fun drawChar(
        glyph: GlyphDescriptor,
        x: Float,
        y: Float,
        z: Float,
        scale: Float,
        color: Color4b,
    ) {
        val renderInfo = glyph.renderInfo
        // We don't need to render whitespaces.
        if (renderInfo.atlasLocation != null && !color.isTransparent) {
            val x1 = x + renderInfo.glyphBounds.xMin * scale
            val y1 = y + renderInfo.glyphBounds.yMin * scale
            val x2 = x + (renderInfo.glyphBounds.xMin + renderInfo.atlasLocation.atlasWidth) * scale
            val y2 = y + (renderInfo.glyphBounds.yMin + renderInfo.atlasLocation.atlasHeight) * scale

            environment.drawTextureQuad(
                glyph.page.texture.glTexture,
                charPos1.set(x1, y1, z),
                renderInfo.atlasLocation.uvCoordinatesOnTexture.min,
                charPos2.set(x2, y2, z),
                renderInfo.atlasLocation.uvCoordinatesOnTexture.max,
                color.toARGB(),
            )
        }
    }

}
