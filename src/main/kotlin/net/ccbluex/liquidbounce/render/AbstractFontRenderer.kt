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
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput
import net.ccbluex.liquidbounce.render.engine.font.HorizontalAnchor
import net.ccbluex.liquidbounce.render.engine.font.VerticalAnchor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence

/**
 * Draws text with minecraft font markup.
 *
 * The [FormattedCharSequence] overloads are the primitives and draw the sequence as it is: no
 * [sanitizeForeignInput], so text that came from a server has to be sanitized by the caller. The
 * [Component] overloads do it for you, at the price of an extra pass per call (flatten the component,
 * match the protected names) - pass a sequence once the text is settled, or when it is measured before
 * it is drawn, to pay that only once.
 */
@AddonApi
abstract class AbstractFontRenderer {

    abstract val size: Float
    abstract val height: Float

    /**
     * Scales this renderer's text metrics to vanilla's 9px GUI font line height.
     */
    val scaleToVanillaFont: Float
        get() = mc.font.lineHeight.toFloat() / this.height

    /**
     * Draws text with minecraft font markup on GUI with [GuiGraphicsExtractor].
     *
     * @return The unscaled width of [text]
     */
    context(ctx: GuiGraphicsExtractor)
    abstract fun draw(text: FormattedCharSequence, parameters: DrawParameters): Float

    /**
     * Draws [text] as it is - unlike the [Component] overloads, it is not sanitized.
     */
    context(ctx: GuiGraphicsExtractor)
    inline fun draw(text: FormattedCharSequence, parameters: DrawParameters.() -> Unit = {}): Float {
        DrawParameters.reset2D()
        parameters(DrawParameters)
        return draw(text, DrawParameters)
    }

    /**
     * Draws a [Component], degenerating its legacy formatting and applying [sanitizeForeignInput] first.
     * Pass a [FormattedCharSequence] when the text is already prepared.
     */
    context(ctx: GuiGraphicsExtractor)
    inline fun draw(text: Component, parameters: DrawParameters.() -> Unit = {}): Float =
        draw(text.sanitizeForeignInput(), parameters)

    /**
     * Draws text with minecraft font markup on GUI with [WorldRenderEnvironment].
     *
     * @return The unscaled width of [text]
     */
    context(ctx: WorldRenderEnvironment)
    abstract fun draw(text: FormattedCharSequence, parameters: DrawParameters): Float

    /**
     * Draws [text] as it is - unlike the [Component] overloads, it is not sanitized.
     */
    context(ctx: WorldRenderEnvironment)
    inline fun draw(text: FormattedCharSequence, parameters: DrawParameters.() -> Unit = {}): Float {
        DrawParameters.reset3D()
        parameters(DrawParameters)
        return draw(text, DrawParameters)
    }

    context(ctx: WorldRenderEnvironment)
    inline fun draw(text: Component, parameters: DrawParameters.() -> Unit = {}): Float =
        draw(text.sanitizeForeignInput(), parameters)

    /**
     * Draws [text] on the GUI at [x]/[y] in GUI pixels; [scale] 1 is this font's own size, the default
     * matches vanilla's. For Kotlin the context overloads with [DrawParameters] do the same.
     *
     * @param color The color of the font when no minecraft-markup applies
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
    ): Float = with(ctx) {
        draw(text) {
            this.x = x
            this.y = y
            this.color = color
            this.shadow = shadow
            this.scale = scale
            this.horizontalAnchor = horizontalAnchor
            this.verticalAnchor = verticalAnchor
        }
    }

    /**
     * Width of [text] in GUI pixels at [scale], the default being vanilla's size.
     */
    @JvmOverloads
    fun getStringWidth(text: Component, scale: Float = scaleToVanillaFont, shadow: Boolean = false): Float =
        getStringWidth(text.sanitizeForeignInput(), shadow) * scale

    /**
     * Approximates the width of a text. Accurate except for obfuscated (`§k`) formatting
     */
    abstract fun getStringWidth(
        text: FormattedCharSequence,
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
     * @param color The color of the characters that carry no color of their own
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

        @JvmField
        var color: Color4b = Color4b.WHITE

        @JvmStatic
        fun reset2D() {
            x = 0f
            y = 0f
            z = Float.NaN
            horizontalAnchor = null
            verticalAnchor = null
            scale = 1f
            shadow = false
            color = Color4b.WHITE
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
            color = Color4b.WHITE
        }
    }

}
