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
package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.render.AbstractFontRenderer.DrawParameters.horizontalAnchor
import net.ccbluex.liquidbounce.render.AbstractFontRenderer.DrawParameters.scale
import net.ccbluex.liquidbounce.render.AbstractFontRenderer.DrawParameters.shadow
import net.ccbluex.liquidbounce.render.AbstractFontRenderer.DrawParameters.verticalAnchor
import net.ccbluex.liquidbounce.render.AbstractFontRenderer.DrawParameters.x
import net.ccbluex.liquidbounce.render.AbstractFontRenderer.DrawParameters.y
import net.ccbluex.liquidbounce.render.AbstractFontRenderer.DrawParameters.z
import net.ccbluex.liquidbounce.render.engine.font.HorizontalAnchor
import net.ccbluex.liquidbounce.render.engine.font.VerticalAnchor
import net.ccbluex.liquidbounce.render.engine.font.processor.ProcessedText
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

@AddonApi
abstract class AbstractFontRenderer<T : ProcessedText> {

    abstract val size: Float
    abstract val height: Float

    /**
     * Scales this renderer's text metrics to vanilla's 9px GUI font line height.
     */
    val scaleToVanillaFont: Float
        get() = mc.font.lineHeight.toFloat() / this.height

    /**
     * Draws a string with minecraft font markup on GUI with [GuiGraphicsExtractor].
     *
     * @return The unscaled width of [text]
     */
    context(ctx: GuiGraphicsExtractor)
    abstract fun draw(text: T, parameters: DrawParameters): Float

    context(ctx: GuiGraphicsExtractor)
    inline fun draw(text: T, parameters: DrawParameters.() -> Unit = {}): Float {
        DrawParameters.reset2D()
        parameters(DrawParameters)
        return draw(text, DrawParameters)
    }

    context(ctx: GuiGraphicsExtractor)
    inline fun draw(text: Component, parameters: DrawParameters.() -> Unit = {}): Float =
        draw(process(text), parameters)

    /**
     * Draws a string with minecraft font markup on GUI with [WorldRenderEnvironment].
     *
     * @return The unscaled width of [text]
     */
    context(ctx: WorldRenderEnvironment)
    abstract fun draw(text: T, parameters: DrawParameters): Float

    context(ctx: WorldRenderEnvironment)
    inline fun draw(text: T, parameters: DrawParameters.() -> Unit = {}): Float {
        DrawParameters.reset3D()
        parameters(DrawParameters)
        return draw(text, DrawParameters)
    }

    context(ctx: WorldRenderEnvironment)
    inline fun draw(text: Component, parameters: DrawParameters.() -> Unit = {}): Float =
        draw(process(text), parameters)

    /**
     * @param defaultColor The color of the font when no minecraft-markup applies
     */
    /**
     * Draws [text] on the GUI at [x]/[y] in GUI pixels; [scale] 1 is this font's own size, the default
     * matches vanilla's. For Kotlin the context overloads with [DrawParameters] do the same.
     *
     * @return the width drawn
     */
    @JvmOverloads
    fun draw(
        ctx: GuiGraphicsExtractor,
        text: Component,
        x: Float,
        y: Float,
        color: Color4b = Color4b.WHITE,
        shadow: Boolean = false,
        scale: Float = scaleToVanillaFont,
        horizontalAnchor: HorizontalAnchor? = null,
        verticalAnchor: VerticalAnchor? = null,
    ): Float {
        val processed = process(text, color)
        return with(ctx) {
            draw(processed) {
                this.x = x
                this.y = y
                this.shadow = shadow
                this.scale = scale
                this.horizontalAnchor = horizontalAnchor
                this.verticalAnchor = verticalAnchor
            }
        }
    }

    /**
     * Width of [text] in GUI pixels at [scale], the default being vanilla's size.
     */
    @JvmOverloads
    fun getStringWidth(text: Component, scale: Float = scaleToVanillaFont, shadow: Boolean = false): Float =
        getStringWidth(process(text), shadow) * scale

    fun process(text: String, defaultColor: Color4b = Color4b.WHITE): T =
        process(text.asPlainText(), defaultColor)

    /**
     * @param defaultColor The color of the font when no minecraft-markup applies
     */
    abstract fun process(text: Component, defaultColor: Color4b = Color4b.WHITE): T

    /**
     * Approximates the width of a text. Accurate except for obfuscated (`§k`) formatting
     */
    abstract fun getStringWidth(
        text: ProcessedText,
        shadow: Boolean = false
    ): Float

    /**
     * @param x Anchor X position
     * @param y Anchor Y position
     * @param z Z offset. [Float.NaN] for 2D rendering
     * @param horizontalAnchor Horizontal anchor of the text, null -> [HorizontalAnchor.START]
     * @param verticalAnchor Vertical anchor of the text, null -> [VerticalAnchor.TOP]
     * @param scale Render scale applied to width and height
     * @param shadow Draw shadow of text
     */
    object DrawParameters {
        @JvmField
        var x: Float = 0f

        @JvmField
        var y: Float = 0f

        @JvmField
        var z: Float = 0f

        @JvmField
        var horizontalAnchor: HorizontalAnchor? = null

        @JvmField
        var verticalAnchor: VerticalAnchor? = null

        @JvmField
        var scale: Float = 1f

        @JvmField
        var shadow: Boolean = false

        @JvmStatic
        fun reset2D() {
            x = 0f
            y = 0f
            z = Float.NaN
            horizontalAnchor = null
            verticalAnchor = null
            scale = 1f
            shadow = false
        }

        @JvmStatic
        fun reset3D() {
            x = 0f
            y = 0f
            z = 0f
            horizontalAnchor = null
            verticalAnchor = null
            scale = 1f
            shadow = false
        }
    }

}
