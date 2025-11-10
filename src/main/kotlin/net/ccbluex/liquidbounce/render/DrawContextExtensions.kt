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
import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.ceilToInt
import net.ccbluex.liquidbounce.utils.client.floorToInt
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.ccbluex.liquidbounce.utils.render.LambdaSimpleGuiElementRenderState
import net.ccbluex.liquidbounce.utils.render.LineGuiElementRenderState
import net.ccbluex.liquidbounce.utils.render.QuadGuiElementRenderState
import net.ccbluex.liquidbounce.utils.render.TriangleGuiElementRenderState
import net.ccbluex.liquidbounce.utils.render.VerticesSetupHandler
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.ScreenRect
import net.minecraft.client.texture.TextureSetup
import net.minecraft.util.math.Vec2f

/**
 * @see net.minecraft.client.gui.render.state.ColoredQuadGuiElementRenderState.createBounds
 */
fun DrawContext.createBounds(x: Float, y: Float, w: Float, h: Float): ScreenRect {
    val rect = ScreenRect(x.floorToInt(), y.floorToInt(), w.ceilToInt(), h.ceilToInt())
        .transformEachVertex(this.matrices)
    return this.scissorStack.peekLast()?.intersection(rect) ?: rect
}

fun DrawContext.createBounds(box: BoundingBox2f): ScreenRect =
    createBounds(box.xMin, box.yMin, box.width, box.height)

@Suppress("NOTHING_TO_INLINE")
inline fun DrawContext.drawCustomElement(
    pipeline: RenderPipeline = RenderPipelines.GUI, // PosColor + QUADS
    textureSetup: TextureSetup = TextureSetup.empty(),
    scissorArea: ScreenRect? = this.scissorStack.peekLast(),
    bounds: ScreenRect? = null,
    verticesSetupHandler: VerticesSetupHandler,
) = this.state.addSimpleElement(
    LambdaSimpleGuiElementRenderState(
        pipeline,
        textureSetup,
        Pools.Mat3x2f.borrow().set(this.matrices),
        scissorArea,
        bounds,
        verticesSetupHandler
    )
)

fun DrawContext.drawLines(
    points: FloatArray,
    argb: Int,
    bounds: ScreenRect,
) {
    this.state.addSimpleElement(
        LineGuiElementRenderState(
            points,
            argb,
            Pools.Mat3x2f.borrow().set(this.matrices),
            this.scissorStack.peekLast(),
            bounds,
        )
    )
}

fun DrawContext.drawQuad(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    fillColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
) {
    val x11 = minOf(x1, x2)
    val y11 = minOf(y1, y2)
    val x21 = maxOf(x1, x2)
    val y21 = maxOf(y1, y2)

    val bounds = createBounds(x11, y11, x21 - x11, y21 - y11)

    if (fillColor != null && !fillColor.isTransparent) {
        this.state.addSimpleElement(
            QuadGuiElementRenderState(
                x11,
                y11,
                x21,
                y21,
                fillColor.toARGB(),
                Pools.Mat3x2f.borrow().set(this.matrices),
                this.scissorStack.peekLast(),
                bounds,
            )
        )
    }
    if (outlineColor != null && !outlineColor.isTransparent) {
        val argb = outlineColor.toARGB()

        drawLines(
            floatArrayOf(
                x11, y11,
                x11, y21,
                x11, y21,
                x21, y21,
                x21, y21,
                x21, y11,
                x21, y11,
                x11, y11,
            ),
            argb,
            bounds,
        )
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

fun DrawContext.drawTriangle(
    p1: Vec2f, p2: Vec2f, p3: Vec2f, color4b: Color4b,
) {
    val argb = color4b.toARGB()
    val minX = minOf(p1.x, p2.x, p3.x)
    val minY = minOf(p1.y, p2.y, p3.y)
    val maxX = maxOf(p1.x, p2.x, p3.x)
    val maxY = maxOf(p1.y, p2.y, p3.y)
    this.state.addSimpleElement(
        TriangleGuiElementRenderState(
            p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, argb,
            Pools.Mat3x2f.borrow().set(this.matrices),
            this.scissorStack.peekLast(),
            createBounds(minX, minY, maxX - minX, maxY - minY),
        )
    )
}
