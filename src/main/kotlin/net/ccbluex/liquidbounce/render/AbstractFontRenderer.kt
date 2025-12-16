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

import net.ccbluex.liquidbounce.render.engine.font.processor.ProcessedText
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text

abstract class AbstractFontRenderer<T : ProcessedText> {
    abstract val size: Float
    abstract val height: Float

    /**
     * Draws a string with minecraft font markup to this object.
     *
     * @param defaultColor The color of the font when no minecraft-markup applies
     * @param shadow Add a shadow to the font?
     * @return The width of the font, without considering the scaling
     */
    @Suppress("LongParameterList")
    context(ctx: DrawContext)
    abstract fun draw(
        text: T,
        x0: Float,
        y0: Float,
        shadow: Boolean = false,
        scale: Float = 1.0f
    ): Float

    fun process(text: String, defaultColor: Color4b = Color4b.WHITE): T = process(text.asPlainText(), defaultColor)
    abstract fun process(text: Text, defaultColor: Color4b = Color4b.WHITE): T

    /**
     * Approximates the width of a text. Accurate except for obfuscated (`§k`) formatting
     */
    abstract fun getStringWidth(
        text: ProcessedText,
        shadow: Boolean = false
    ): Float

    val ProcessedText.width: Float
        get() = getStringWidth(this, false)

    val ProcessedText.widthWithShadow: Float
        get() = getStringWidth(this, true)
}
