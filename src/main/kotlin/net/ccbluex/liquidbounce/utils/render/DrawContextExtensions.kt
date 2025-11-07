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

package net.ccbluex.liquidbounce.utils.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.ScreenRect
import net.minecraft.client.gui.render.state.ColoredQuadGuiElementRenderState
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.texture.TextureSetup
import org.joml.Matrix3x2f

/**
 * Float version of [DrawContext.fill]
 */
@Suppress("LongParameterList")
fun DrawContext.fill(x1: Float, y1: Float, x2: Float, y2: Float, color: Int) =
    fill(RenderPipelines.GUI, x1, y1, x2, y2, color)

@Suppress("LongParameterList")
fun DrawContext.fill(pipeline: RenderPipeline, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
    val x1 = minOf(x1, x2)
    val y1 = minOf(y1, y2)

    val x2 = maxOf(x1, x2)
    val y2 = maxOf(y1, y2)

    this.fill(pipeline, TextureSetup.empty(), x1, y1, x2, y2, color, null as Int?)
}

private fun DrawContext.fill(
    pipeline: RenderPipeline,
    textureSetup: TextureSetup,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    color: Int,
    color2: Int?
) {
    this.state.addSimpleElement(
        ColoredQuadGuiElementRenderStateF(
            pipeline,
            textureSetup,
            Matrix3x2f(this.matrices),
            x1,
            y1,
            x2,
            y2,
            color,
            color2 ?: color,
            this.scissorStack.peekLast()
        )
    )
}

/**
 * Float version of [DrawContext.drawHorizontalLine]
 */
fun DrawContext.drawHorizontalLine(x1: Float, x2: Float, y: Float, thickness: Float, color: Int) {
    this.fill(x1, y, x2, y + thickness, color)
}

/**
 * Float version of [DrawContext.drawVerticalLine]
 */
fun DrawContext.drawVerticalLine(x: Float, y1: Float, y2: Float, thickness: Float, color: Int) {
    this.fill(x, y1, x + thickness, y2, color)
}
