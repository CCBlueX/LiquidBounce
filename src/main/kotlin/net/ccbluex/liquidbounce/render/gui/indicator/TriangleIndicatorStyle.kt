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

package net.ccbluex.liquidbounce.render.gui.indicator

import net.ccbluex.liquidbounce.render.drawTriangle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.Mth

/**
 * A filled triangle with its base at the local origin and its tip at `(0, height)`, which is the shape
 * [IndicatorRenderer] places on the anchor.
 *
 * @param tailConcaveSize how far the base is notched inwards, `0` for a plain triangle.
 */
data class TriangleIndicatorStyle(
    val width: Float,
    val height: Float,
    val tailConcaveSize: Float = 0.0F,
) : IndicatorStyle {

    override val length: Float
        get() = height

    override fun extract(ctx: GuiGraphicsExtractor, color: Color4b) {
        if (Mth.equal(tailConcaveSize, 0.0F)) {
            ctx.drawTriangle(
                x0 = -width * 0.5F, y0 = 0.0F,
                x1 = 0.0F, y1 = height,
                x2 = width * 0.5F, y2 = 0.0F,
                fillColor = color,
                cull = false,
            )
        } else {
            ctx.drawTriangle(
                x0 = -width * 0.5F, y0 = 0.0F,
                x1 = 0.0F, y1 = height,
                x2 = 0.0F, y2 = tailConcaveSize,
                fillColor = color,
                cull = false,
            ) // left
            ctx.drawTriangle(
                x0 = 0.0F, y0 = tailConcaveSize,
                x1 = 0.0F, y1 = height,
                x2 = width * 0.5F, y2 = 0.0F,
                fillColor = color,
                cull = false,
            ) // right
        }
    }

}
