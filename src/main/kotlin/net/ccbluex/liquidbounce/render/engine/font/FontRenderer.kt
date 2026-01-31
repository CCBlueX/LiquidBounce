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

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntStack
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput
import net.ccbluex.liquidbounce.render.AbstractFontRenderer
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.FontFace
import net.ccbluex.liquidbounce.render.FontManager.DEFAULT_FONT_SIZE
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.setColor
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.drawCustomMeshTextured
import net.ccbluex.liquidbounce.render.drawGlyphOnCurrentLayer
import net.ccbluex.liquidbounce.render.drawHorizontalLine
import net.ccbluex.liquidbounce.render.engine.font.processor.MinecraftTextProcessor
import net.ccbluex.liquidbounce.render.engine.font.processor.ProcessedText
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.textureSetup
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import java.awt.Font

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
    val font: FontFace,
    val glyphManager: FontGlyphPageManager,
    override val size: Float = DEFAULT_FONT_SIZE
) : AbstractFontRenderer<MinecraftTextProcessor.RecyclingProcessedText>() {

    // Caches
    private val underlinesIdxStack = IntArrayList()
    private val strikethroughIdxStack = IntArrayList()

    override val height: Float = font.plainStyle.height

    private val ascent: Float = font.plainStyle.ascent

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

    override fun process(text: Component, defaultColor: Color4b): MinecraftTextProcessor.RecyclingProcessedText {
        return MinecraftTextProcessor.process(text.sanitizeForeignInput(), defaultColor)
    }

    context(ctx: GuiGraphics)
    override fun draw(
        text: MinecraftTextProcessor.RecyclingProcessedText,
        parameters: DrawParameters,
    ): Float = commonDraw(text, parameters)

    context(ctx: WorldRenderEnvironment)
    override fun draw(
        text: MinecraftTextProcessor.RecyclingProcessedText,
        parameters: DrawParameters,
    ): Float = commonDraw(text, parameters)

    context(ctx: Any)
    @Suppress("CognitiveComplexMethod")
    private fun commonDraw(
        text: MinecraftTextProcessor.RecyclingProcessedText,
        parameters: DrawParameters,
    ): Float {
        val scale = parameters.scale

        val x = parameters.horizontalAnchor?.anchorToDrawX(
            x = parameters.x,
            width = getStringWidth(text, parameters.shadow),
            scale,
        ) ?: parameters.x

        val y = parameters.verticalAnchor?.anchorToDrawY(
            y = parameters.y,
            height,
            scale,
        ) ?: parameters.y

        val z = parameters.z

        var len = 0.0f

        if (parameters.shadow) {
            len = drawInternal(
                text,
                posX = x + 2.0f * scale,
                posY = y + 2.0f * scale,
                posZ = z,
                scale,
                overrideColor = shadowColor
            )
        }

        len = maxOf(
            len,
            drawInternal(text, x, y, if (z.isNaN()) z else z + 0.001f, scale, overrideColor = null)
        )

        MinecraftTextProcessor.TEXT_POOL.recycle(text)

        return len
    }

    /**
     * @param ctx [GuiGraphics] or [WorldRenderEnvironment]
     * @param posZ if it's [Float.NaN], then use 2D rendering; or else use 3D rendering
     *
     * @return The resulting x value
     */
    context(ctx: Any)
    @Suppress("CognitiveComplexMethod")
    private fun drawInternal(
        text: ProcessedText,
        posX: Float,
        posY: Float,
        posZ: Float,
        scale: Float,
        overrideColor: Color4b?,
    ): Float {
        if (text.chars.isEmpty()) {
            return posX
        }

        val underlineStack = loadUnderlines(text)
        val strikethroughStack = loadStrikethroughs(text)

        var x = posX
        var y = posY + this.ascent * scale
        var color: Color4b? = null

        var strikeThroughStartX: Float = Float.NaN
        var underlineStartX: Float = Float.NaN

        val fallbackGlyph = this.glyphManager.getFallbackGlyph(this.font)

        text.chars.forEachIndexed { charIdx, processedChar ->
            val glyph = this.glyphManager.requestGlyph(this.font, processedChar.font, processedChar.char)
                ?: fallbackGlyph
            color = overrideColor ?: processedChar.color

            if (!underlineStack.isEmpty && underlineStack.topInt() == charIdx) {
                underlineStack.popInt()
                underlineStartX = x
            }
            if (!strikethroughStack.isEmpty && strikethroughStack.topInt() == charIdx) {
                strikethroughStack.popInt()
                strikeThroughStartX = x
            }

            drawChar(glyph, x, y, posZ, scale, color)

            val layoutInfo =
                if (!processedChar.obfuscated) glyph.renderInfo.layoutInfo else fallbackGlyph.renderInfo.layoutInfo

            x += layoutInfo.advanceX * scale
            y += layoutInfo.advanceY * scale

            if (!underlineStack.isEmpty && underlineStack.topInt() == charIdx) {
                underlineStack.popInt()
                drawLine(underlineStartX, x, y, posZ, color, false)
            }

            if (!strikethroughStack.isEmpty && strikethroughStack.topInt() == charIdx) {
                strikethroughStack.popInt()
                drawLine(strikeThroughStartX, x, y, posZ, color, true)
            }
        }

        if (!underlineStack.isEmpty && !underlineStartX.isNaN()) {
            underlineStack.popInt()
            drawLine(underlineStartX, x, y, posZ, color!!, false)
        }

        if (!strikethroughStack.isEmpty && !strikeThroughStartX.isNaN()) {
            strikethroughStack.popInt()
            drawLine(strikeThroughStartX, x, y, posZ, color!!, true)
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

    context(ctx: Any)
    private fun drawLine(
        x0: Float,
        x1: Float,
        y: Float,
        z: Float,
        color: Color4b,
        through: Boolean
    ) {
        val y = if (through) y - this.height * 0.85f + this.ascent else y + 1f
        if (z.isNaN()) {
            (ctx as GuiGraphics).drawHorizontalLine(x0, x1, y, 1f, color)
        } else {
            (ctx as WorldRenderEnvironment).drawCustomMesh(ClientRenderPipelines.Quads) { matrix ->
                val y0 = y
                val y1 = y + 1f
                addVertex(matrix, x0, y0, z).setColor(color)
                addVertex(matrix, x0, y1, z).setColor(color)
                addVertex(matrix, x1, y1, z).setColor(color)
                addVertex(matrix, x1, y0, z).setColor(color)
            }
        }
    }

    context(ctx: Any)
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
            val x0 = x + renderInfo.glyphBounds.xMin * scale
            val y0 = y + renderInfo.glyphBounds.yMin * scale
            val x1 = x + (renderInfo.glyphBounds.xMin + renderInfo.atlasLocation.atlasWidth) * scale
            val y1 = y + (renderInfo.glyphBounds.yMin + renderInfo.atlasLocation.atlasHeight) * scale
            val uv1 = renderInfo.atlasLocation.uvCoordinatesOnTexture.min
            val uv2 = renderInfo.atlasLocation.uvCoordinatesOnTexture.max
            val argb = color.argb

            if (z.isNaN()) {
                (ctx as GuiGraphics).drawGlyphOnCurrentLayer(
                    glyph.page.texture.textureSetup,
                    x0 = x0, y0 = y0, x1 = x1, y1 = y1,
                    u1 = uv1.u, v1 = uv1.v, u2 = uv2.u, v2 = uv2.v, argb = argb,
                )
            } else {
                (ctx as WorldRenderEnvironment).drawCustomMeshTextured(glyph.page.texture) { matrix ->
                    addVertex(matrix, x0, y0, z)
                        .setUv(uv1.u, uv1.v)
                        .setColor(argb)
                    addVertex(matrix, x0, y1, z)
                        .setUv(uv1.u, uv2.v)
                        .setColor(argb)
                    addVertex(matrix, x1, y1, z)
                        .setUv(uv2.u, uv2.v)
                        .setColor(argb)
                    addVertex(matrix, x1, y0, z)
                        .setUv(uv2.u, uv1.v)
                        .setColor(argb)
                }
            }
        }
    }

}
