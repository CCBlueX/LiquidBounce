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
package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.render.engine.font.HorizontalAnchor
import net.ccbluex.liquidbounce.render.engine.font.VerticalAnchor
import net.ccbluex.liquidbounce.render.engine.font.processor.ProcessedText
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

abstract class AbstractFontRenderer<T : ProcessedText> {

    abstract val size: Float
    abstract val height: Float

    /**
     * Draws a string with minecraft font markup.
     *
     * @param x Anchor X position
     * @param y Anchor Y position
     * @param horizontalAnchor Horizontal anchor of the text, null -> [HorizontalAnchor.START]
     * @param verticalAnchor Vertical anchor of the text, null -> [VerticalAnchor.TOP]
     * @param scale Render scale applied to width and height
     * @param shadow Draw shadow of text
     *
     * @return The unscaled width of [text]
     */
    context(ctx: GuiGraphics)
    @Suppress("LongParameterList")
    abstract fun draw(
        text: T,
        x: Float = 0f,
        y: Float = 0f,
        horizontalAnchor: HorizontalAnchor? = null,
        verticalAnchor: VerticalAnchor? = null,
        scale: Float = 1.0f,
        shadow: Boolean = false,
    ): Float

    /**
     * @param defaultColor The color of the font when no minecraft-markup applies
     */
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

}
