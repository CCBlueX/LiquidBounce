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

import com.mojang.blaze3d.pipeline.RenderPipeline
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.ceilToInt
import net.ccbluex.liquidbounce.utils.client.floorToInt
import net.ccbluex.liquidbounce.utils.render.LambdaSimpleGuiElementRenderState
import net.ccbluex.liquidbounce.utils.render.VerticesSetupHandler
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.ScreenRect
import net.minecraft.client.texture.TextureSetup
import org.joml.Matrix3x2f

/**
 * @see net.minecraft.client.gui.render.state.ColoredQuadGuiElementRenderState.createBounds
 */
fun DrawContext.createBounds(x: Float, y: Float, w: Float, h: Float): ScreenRect {
    val rect = ScreenRect(x.floorToInt(), y.floorToInt(), w.ceilToInt(), h.ceilToInt())
        .transformEachVertex(this.matrices)
    return this.scissorStack.peekLast()?.intersection(rect) ?: rect
}

inline fun DrawContext.drawCustomElement(
    pipeline: RenderPipeline = RenderPipelines.GUI,
    textureSetup: TextureSetup = TextureSetup.empty(),
    scissorArea: ScreenRect? = this.scissorStack.peekLast(),
    bounds: ScreenRect? = null,
    verticesSetupHandler: VerticesSetupHandler,
) = this.state.addSimpleElement(
    LambdaSimpleGuiElementRenderState(
        pipeline,
        textureSetup,
        Matrix3x2f(this.matrices),
        scissorArea,
        bounds,
        verticesSetupHandler
    )
)

fun DrawContext.drawQuad(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    fillColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
) {
    val x1 = minOf(x1, x2)
    val y1 = minOf(y1, y2)
    val x2 = maxOf(x1, x2)
    val y2 = maxOf(y1, y2)

    if (fillColor != null && !fillColor.isTransparent) {
        val argb = fillColor.toARGB()
        drawCustomElement(
            pipeline = RenderPipelines.GUI,
            bounds = createBounds(x1, y1, x2 - x1, y2 - y1),
        ) { pose, depth ->
            vertex(pose, x1, y1, depth).color(argb)
            vertex(pose, x1, y2, depth).color(argb)
            vertex(pose, x2, y2, depth).color(argb)
            vertex(pose, x2, y1, depth).color(argb)
        }
    }
    if (outlineColor != null && !outlineColor.isTransparent) {
        val argb = outlineColor.toARGB()
        drawCustomElement(
            pipeline = ClientRenderPipelines.GUI.Lines,
            bounds = createBounds(x1, y1, x2 - x1, y2 - y1),
        ) { pose, depth ->
            vertex(pose, x1, y1, depth).color(argb)
            vertex(pose, x1, y2, depth).color(argb)
            vertex(pose, x1, y2, depth).color(argb)
            vertex(pose, x2, y2, depth).color(argb)
            vertex(pose, x2, y2, depth).color(argb)
            vertex(pose, x2, y1, depth).color(argb)
            vertex(pose, x2, y1, depth).color(argb)
            vertex(pose, x1, y1, depth).color(argb)
        }
    }
}

/**
 * Float version of [DrawContext.drawHorizontalLine]
 */
fun DrawContext.drawHorizontalLine(x1: Float, x2: Float, y: Float, thickness: Float, color: Color4b) {
    this.drawQuad(x1, y, x2, y + thickness, color)
}

/**
 * Float version of [DrawContext.drawVerticalLine]
 */
fun DrawContext.drawVerticalLine(x: Float, y1: Float, y2: Float, thickness: Float, color: Color4b) {
    this.drawQuad(x, y1, x + thickness, y2, color)
}
