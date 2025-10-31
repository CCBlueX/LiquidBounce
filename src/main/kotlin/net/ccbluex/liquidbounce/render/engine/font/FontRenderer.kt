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

import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.FontManager.DEFAULT_FONT_SIZE
import net.ccbluex.liquidbounce.render.engine.font.processor.MinecraftTextProcessor
import net.ccbluex.liquidbounce.render.engine.font.processor.ProcessedText
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.collection.Pools
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

    private val positionCache = Vector3f()
    private val underlinesCache = ArrayDeque<IntRange>()
    private val strikethroughCache = ArrayDeque<IntRange>()

    override val height: Float = font.styles.firstNotNullOf { it?.height }

    private val ascent: Float = font.styles.firstNotNullOf { it?.ascent }

    private val shadowColor = Color4b(0, 0, 0, 150)

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
    @Suppress("CognitiveComplexMethod")
    context(environment: GUIRenderEnvironment)
    private fun drawInternal(
        text: ProcessedText,
        pos: Vector3fc,
        scale: Float,
        overrideColor: Color4b? = null
    ): Float {
        if (text.chars.isEmpty()) {
            return pos.x()
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

        var x = pos.x()
        var y = pos.y() + this.ascent * scale

        var strikeThroughStartX: Float? = null
        var underlineStartX: Float? = null

        val fallbackGlyph = this.glyphManager.getFallbackGlyph(this.font)

        val vec3f1 = Pools.Vec3f.borrow()
        val vec3f2 = Pools.Vec3f.borrow()

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
                val x1 = x + renderInfo.glyphBounds.xMin * scale
                val y1 = y + renderInfo.glyphBounds.yMin * scale
                val x2 = x + (renderInfo.glyphBounds.xMin + atlasLocation.atlasWidth) * scale
                val y2 = y + (renderInfo.glyphBounds.yMin + atlasLocation.atlasHeight) * scale

                environment.drawTextureQuad(
                    glyph.page.texture.glTexture,
                    vec3f1.set(x1, y1, pos.z()),
                    atlasLocation.uvCoordinatesOnTexture.min,
                    vec3f2.set(x2, y2, pos.z()),
                    atlasLocation.uvCoordinatesOnTexture.max,
                    color.toARGB(),
                )
            }

            val layoutInfo =
                if (!processedChar.obfuscated) renderInfo.layoutInfo else fallbackGlyph.renderInfo.layoutInfo

            x += layoutInfo.advanceX * scale
            y += layoutInfo.advanceY * scale

            if (underlineStack.isNotEmpty() && underlineStack.first().last == charIdx) {
                underlineStack.removeFirst()

                drawLine(underlineStartX!!, x, y, pos.z(), color, false)
            }

            if (strikethroughStack.isNotEmpty() && strikethroughStack.first().last == charIdx) {
                strikethroughStack.removeFirst()

                drawLine(strikeThroughStartX!!, x, y, pos.z(), color, true)
            }
        }

        Pools.Vec3f.recycle(vec3f1)
        Pools.Vec3f.recycle(vec3f2)

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

}
