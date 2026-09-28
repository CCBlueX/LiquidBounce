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

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.liquidbounce.render.AbstractFontRenderer
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.FontFace
import net.ccbluex.liquidbounce.render.FontManager.DEFAULT_FONT_SIZE
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.copyPosePooled
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.drawCustomMeshTextured
import net.ccbluex.liquidbounce.render.drawHorizontalLine
import net.ccbluex.liquidbounce.render.engine.font.MinecraftTextProcessor.RecyclingProcessedText
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.element.TextRunGuiElementRenderState
import net.ccbluex.liquidbounce.render.setColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.ARGB
import net.minecraft.util.FormattedCharSequence
import java.awt.Font

@Suppress("TooManyFunctions")
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
) : AbstractFontRenderer() {

    /**
     * Glyphs of the text currently being drawn, refilled by [resolveGlyphs].
     */
    private val resolvedGlyphs = ObjectArrayList<GlyphDescriptor>()

    /**
     * Geometry of the glyph currently being submitted: `x0, y0, x1, y1, u1, v1, u2, v2`, refilled by
     * [resolveQuad].
     */
    private val quad = FloatArray(8)

    override val height: Float = font.plainStyle.height

    private val ascent: Float = font.plainStyle.ascent

    private val underlineOffset: Float = font.plainStyle.underlineOffset
    private val underlineThickness: Float = font.plainStyle.underlineThickness
    private val strikethroughOffset: Float = font.plainStyle.strikethroughOffset
    private val strikethroughThickness: Float = font.plainStyle.strikethroughThickness

    private val shadowColor = Color4b(0, 0, 0, 150)

    context(ctx: GuiGraphicsExtractor)
    override fun draw(
        text: FormattedCharSequence,
        parameters: DrawParameters,
    ): Float = commonDraw(text, parameters)

    context(ctx: WorldRenderEnvironment)
    override fun draw(
        text: FormattedCharSequence,
        parameters: DrawParameters,
    ): Float = commonDraw(text, parameters)

    @Suppress("CognitiveComplexMethod")
    context(ctx: Any)
    private fun commonDraw(
        text: FormattedCharSequence,
        parameters: DrawParameters,
    ): Float {
        val processed = MinecraftTextProcessor.process(text, parameters.color)
        val glyphs = resolveGlyphs(processed)
        val scale = parameters.scale
        val width = getStringWidth(processed, glyphs, parameters.shadow)

        val x = parameters.horizontalAnchor?.anchorToDrawX(
            x = parameters.x,
            width = width,
            scale,
        ) ?: parameters.x

        val y = parameters.verticalAnchor?.anchorToDrawY(
            y = parameters.y,
            height,
            scale,
        ) ?: parameters.y

        val z = parameters.z

        if (parameters.shadow) {
            drawInternal(
                processed,
                glyphs,
                posX = x + 2.0f * scale,
                posY = y + 2.0f * scale,
                posZ = z,
                scale,
                overrideColor = shadowColor
            )
        }

        drawInternal(processed, glyphs, x, y, if (z.isNaN()) z else z + 0.001f, scale, overrideColor = null)

        MinecraftTextProcessor.TEXT_POOL.recycle(processed)

        return width
    }

    /**
     * @param ctx [GuiGraphicsExtractor] or [WorldRenderEnvironment]
     * @param posZ if it's [Float.NaN], then use 2D rendering; or else use 3D rendering
     *
     */
    @Suppress("CognitiveComplexMethod")
    context(ctx: Any)
    private fun drawInternal(
        text: RecyclingProcessedText,
        glyphs: List<GlyphDescriptor>,
        posX: Float,
        posY: Float,
        posZ: Float,
        scale: Float,
        overrideColor: Color4b?,
    ) {
        if (text.chars.isEmpty()) {
            return
        }

        val overrideArgb = overrideColor?.argb
        var x = posX
        var y = posY + this.ascent * scale

        // Decorations belong to the characters now, so a run lasts as long as the characters keep
        // sharing their style and colour. No style is negative, so the first character starts a run.
        var runStyle = -1
        var runColor = 0
        var underlineStartX: Float = Float.NaN
        var strikeThroughStartX: Float = Float.NaN

        val fallbackGlyph = this.glyphManager.getFallbackGlyph(this.font)

        // 2D glyphs join a run of quads per atlas page instead of becoming one element each.
        var run: TextRunGuiElementRenderState? = null

        fun flushRun() {
            val pending = run ?: return

            run = null
            (ctx as GuiGraphicsExtractor).guiRenderState.addGlyphToCurrentLayer(pending)
        }

        fun appendQuad(glyph: GlyphDescriptor, argb: Int) {
            val gui = ctx as GuiGraphicsExtractor
            val page = glyph.page
            val scissorArea = gui.scissorStack.peek()

            val current = run?.takeIf { it.accepts(page, scissorArea) } ?: TextRunGuiElementRenderState(
                ClientRenderPipelines.GUI.FontMask, page, gui.copyPosePooled(), scissorArea,
            ).also {
                flushRun()
                run = it
            }

            current.addQuad(quad, argb)
        }

        text.forEachIndexed { charIdx, processedChar ->
            val glyph = glyphs[charIdx]
            val style = processedChar.style
            val charColor = overrideArgb ?: processedChar.color

            // A new run ends the previous one at this character's left edge.
            if (style != runStyle || charColor != runColor) {
                if (!underlineStartX.isNaN()) {
                    drawLine(underlineStartX, x, y, posZ, scale, runColor, false)
                }
                if (!strikeThroughStartX.isNaN()) {
                    drawLine(strikeThroughStartX, x, y, posZ, scale, runColor, true)
                }

                runStyle = style
                runColor = charColor
                underlineStartX = if (processedChar.underlined) x else Float.NaN
                strikeThroughStartX = if (processedChar.strikethrough) x else Float.NaN
            }

            // We don't need to render whitespaces.
            if (ARGB.alpha(charColor) != 0 && resolveQuad(glyph, x, y, scale)) {
                if (posZ.isNaN()) {
                    appendQuad(glyph, charColor)
                } else {
                    submitQuadMesh(glyph, posZ, charColor)
                }
            }

            val layoutInfo =
                if (!processedChar.obfuscated) glyph.renderInfo.layoutInfo else fallbackGlyph.renderInfo.layoutInfo

            x += layoutInfo.advanceX * scale
            y += layoutInfo.advanceY * scale
        }

        if (!underlineStartX.isNaN()) {
            drawLine(underlineStartX, x, y, posZ, scale, runColor, false)
        }

        if (!strikeThroughStartX.isNaN()) {
            drawLine(strikeThroughStartX, x, y, posZ, scale, runColor, true)
        }

        flushRun()
    }

    override fun getStringWidth(
        text: FormattedCharSequence,
        shadow: Boolean
    ): Float {
        val processed = MinecraftTextProcessor.process(text, Color4b.WHITE)
        val width = getStringWidth(processed, resolveGlyphs(processed), shadow)

        MinecraftTextProcessor.TEXT_POOL.recycle(processed)

        return width
    }

    private fun getStringWidth(
        text: RecyclingProcessedText,
        glyphs: List<GlyphDescriptor>,
        shadow: Boolean,
    ): Float {
        if (text.chars.isEmpty()) {
            return 0.0f
        }

        var x = 0.0f

        val fallbackLayoutInfo = this.glyphManager.getFallbackGlyph(this.font).renderInfo.layoutInfo

        text.forEachIndexed { index, processedChar ->
            val layoutInfo =
                if (!processedChar.obfuscated) glyphs[index].renderInfo.layoutInfo else fallbackLayoutInfo

            x += layoutInfo.advanceX
        }

        return if (shadow) {
            x + 2.0f
        } else {
            x
        }
    }

    /**
     * Resolves every character of [text] to the glyph it will be drawn with.
     *
     * A draw needs them three times - once for the width and once per shadow pass - so they are
     * resolved here to keep the codepoint lookups at one per character.
     *
     * The returned list is reused, so it is only valid until the next call.
     */
    private fun resolveGlyphs(text: RecyclingProcessedText): ObjectArrayList<GlyphDescriptor> {
        val glyphs = this.resolvedGlyphs

        glyphs.clear()
        glyphs.ensureCapacity(text.chars.size)

        val fallbackGlyph = this.glyphManager.getFallbackGlyph(this.font)

        text.forEach { processedChar ->
            val glyph = this.glyphManager.requestGlyph(this.font, processedChar.font, processedChar.codepoint)

            glyphs.add(glyph ?: fallbackGlyph)
        }

        return glyphs
    }

    context(ctx: Any)
    private fun drawLine(
        x0: Float,
        x1: Float,
        y: Float,
        z: Float,
        scale: Float,
        argb: Int,
        through: Boolean
    ) {
        val lineWidth = if (through) {
            strikethroughThickness * scale
        } else {
            underlineThickness * scale
        }.coerceAtLeast(0f)
        val lineY = y + if (through) strikethroughOffset * scale else underlineOffset * scale
        if (z.isNaN()) {
            (ctx as GuiGraphicsExtractor).drawHorizontalLine(x0, x1, lineY, lineWidth, Color4b(argb))
        } else {
            (ctx as WorldRenderEnvironment).drawCustomMesh(ClientRenderPipelines.quads(noDepthTest = true)) { matrix ->
                val y0 = lineY
                val y1 = lineY + lineWidth
                addVertex(matrix, x0, y0, z).setColor(argb)
                addVertex(matrix, x0, y1, z).setColor(argb)
                addVertex(matrix, x1, y1, z).setColor(argb)
                addVertex(matrix, x1, y0, z).setColor(argb)
            }
        }
    }

    /**
     * Fills [quad] with the geometry of [glyph] drawn at [x]/[y], returning false when the glyph has
     * nothing to draw.
     */
    private fun resolveQuad(glyph: GlyphDescriptor, x: Float, y: Float, scale: Float): Boolean {
        val renderInfo = glyph.renderInfo
        val atlasLocation = renderInfo.atlasLocation ?: return false
        val glyphBounds = renderInfo.glyphBounds
        val quad = this.quad

        quad[0] = x + glyphBounds.xMin * scale
        quad[1] = y + glyphBounds.yMin * scale
        quad[2] = x + (glyphBounds.xMin + atlasLocation.atlasWidth) * scale
        quad[3] = y + (glyphBounds.yMin + atlasLocation.atlasHeight) * scale

        val uv = atlasLocation.uvCoordinatesOnTexture
        quad[4] = uv.min.u
        quad[5] = uv.min.v
        quad[6] = uv.max.u
        quad[7] = uv.max.v

        return true
    }

    /**
     * Submits [quad] as its own mesh, which 3D text needs because it cannot join a GUI element.
     */
    context(ctx: Any)
    private fun submitQuadMesh(glyph: GlyphDescriptor, z: Float, argb: Int) {
        val quad = this.quad

        (ctx as WorldRenderEnvironment).drawCustomMeshTextured(
            glyph.page.texture,
            pipeline = ClientRenderPipelines.FontMaskQuads,
        ) { matrix ->
            addVertex(matrix, quad[0], quad[1], z)
                .setUv(quad[4], quad[5])
                .setColor(argb)
            addVertex(matrix, quad[0], quad[3], z)
                .setUv(quad[4], quad[7])
                .setColor(argb)
            addVertex(matrix, quad[2], quad[3], z)
                .setUv(quad[6], quad[7])
                .setColor(argb)
            addVertex(matrix, quad[2], quad[1], z)
                .setUv(quad[6], quad[5])
                .setColor(argb)
        }
    }

}
