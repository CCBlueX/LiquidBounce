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

@file:Suppress("detekt:TooManyFunctions", "NOTHING_TO_INLINE")

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import it.unimi.dsi.fastutil.floats.Float2IntFunction
import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.ceilToInt
import net.ccbluex.liquidbounce.utils.client.floorToInt
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.ccbluex.liquidbounce.utils.render.CircleGuiElementRenderState
import net.ccbluex.liquidbounce.utils.render.LambdaSimpleGuiElementRenderState
import net.ccbluex.liquidbounce.utils.render.LineGuiElementRenderState
import net.ccbluex.liquidbounce.utils.render.QuadGuiElementRenderState
import net.ccbluex.liquidbounce.utils.render.TexQuadGuiElementRenderState
import net.ccbluex.liquidbounce.utils.render.TriangleGuiElementRenderState
import net.ccbluex.liquidbounce.utils.render.VerticesSetupHandler
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.BlitRenderState
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.world.phys.Vec2
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fStack
import org.joml.Vector2f

private val LEFT_TOP = Vector2f()
private val RIGHT_TOP = Vector2f()
private val LEFT_BOTTOM = Vector2f()
private val RIGHT_BOTTOM = Vector2f()

/**
 * Primitive version of [ScreenRectangle.transformMaxBounds]
 */
private fun Matrix3x2f.transformEachVertex(
    sameAxis: Int, otherAxis: Int, width: Int, height: Int,
): ScreenRectangle {
    val left = sameAxis
    val right = sameAxis + width
    val top = otherAxis
    val bottom = otherAxis + height

    val v1 = transformPosition(left.toFloat(), top.toFloat(), LEFT_TOP)
    val v2 = transformPosition(right.toFloat(), top.toFloat(), RIGHT_TOP)
    val v3 = transformPosition(left.toFloat(), bottom.toFloat(), LEFT_BOTTOM)
    val v4 = transformPosition(right.toFloat(), bottom.toFloat(), RIGHT_BOTTOM)
    val minX = minOf(v1.x, minOf(v3.x, v2.x, v4.x))
    val maxX = maxOf(v1.x, maxOf(v3.x, v2.x, v4.x))
    val minY = minOf(v1.y, minOf(v3.y, v2.y, v4.y))
    val maxY = maxOf(v1.y, maxOf(v3.y, v2.y, v4.y))
    return ScreenRectangle(minX.floorToInt(), minY.floorToInt(), (maxX - minX).ceilToInt(), (maxY - minY).ceilToInt())
}

/**
 * @see net.minecraft.client.gui.render.state.ColoredRectangleRenderState.getBounds
 */
fun GuiGraphics.createBounds(x: Float, y: Float, w: Float, h: Float): ScreenRectangle {
//    val rect = ScreenRect(x.floorToInt(), y.floorToInt(), w.ceilToInt(), h.ceilToInt())
//        .transformEachVertex(this.matrices)
    val rect = this.pose().transformEachVertex(
        x.floorToInt(), y.floorToInt(), w.ceilToInt(), h.ceilToInt()
    )
    return this.scissorStack.peek()?.intersection(rect) ?: rect
}

fun GuiGraphics.createBounds(box: BoundingBox2f): ScreenRectangle =
    createBounds(box.xMin, box.yMin, box.width, box.height)

inline fun GuiGraphics.copyPose(): Matrix3x2f = Pools.Mat3x2f.borrow().set(this.pose())

inline fun Matrix3x2fStack.withPush(block: Matrix3x2fStack.() -> Unit) {
    pushMatrix()
    try {
        block()
    } finally {
        popMatrix()
    }
}

inline fun GuiGraphics.ScissorStack.withPush(rect: ScreenRectangle, block: GuiGraphics.ScissorStack.() -> Unit) {
    push(rect)
    try {
        block()
    } finally {
        pop()
    }
}

inline fun GuiGraphics.drawCustomElement(
    pipeline: RenderPipeline = RenderPipelines.GUI, // PosColor + QUADS
    textureSetup: TextureSetup = TextureSetup.noTexture(),
    scissorArea: ScreenRectangle? = this.scissorStack.peek(),
    bounds: ScreenRectangle? = null,
    verticesSetupHandler: VerticesSetupHandler,
) = this.guiRenderState.submitGuiElement(
    LambdaSimpleGuiElementRenderState(
        pipeline,
        textureSetup,
        copyPose(),
        scissorArea,
        bounds,
        verticesSetupHandler
    )
)

fun GuiGraphics.drawLines(
    points: FloatArray,
    argb: Int,
    bounds: ScreenRectangle,
    cull: Boolean = true,
) {
    this.guiRenderState.submitGuiElement(
        LineGuiElementRenderState(
            points,
            argb,
            ClientRenderPipelines.GUI.lines(cull),
            copyPose(),
            this.scissorStack.peek(),
            bounds,
        )
    )
}

fun GuiGraphics.drawQuad(
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
        this.guiRenderState.submitGuiElement(
            QuadGuiElementRenderState(
                x11,
                y11,
                x21,
                y21,
                fillColor.argb,
                copyPose(),
                this.scissorStack.peek(),
                bounds,
            )
        )
    }
    if (outlineColor != null && !outlineColor.isTransparent) {
        val argb = outlineColor.argb

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
 * Float version of [GuiGraphics.drawHorizontalLine]
 */
fun GuiGraphics.drawHorizontalLine(x1: Float, x2: Float, y: Float, thickness: Float, color: Color4b) {
    this.drawQuad(x1, y, x2, y + thickness, color)
}

/**
 * Float version of [GuiGraphics.drawVerticalLine]
 */
fun GuiGraphics.drawVerticalLine(x: Float, y1: Float, y2: Float, thickness: Float, color: Color4b) {
    this.drawQuad(x, y1, x + thickness, y2, color)
}

@Suppress("LongParameterList")
fun GuiGraphics.drawTriangle(
    x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float,
    fillColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
    cull: Boolean = true,
) {
    val minX = minOf(x0, x1, x2)
    val minY = minOf(y0, y1, y2)
    val maxX = maxOf(x0, x1, x2)
    val maxY = maxOf(y0, y1, y2)
    val bounds = createBounds(minX, minY, maxX - minX, maxY - minY)

    if (fillColor != null && !fillColor.isTransparent) {
        this.guiRenderState.submitGuiElement(
            TriangleGuiElementRenderState(
                x0, y0, x1, y1, x2, y2,
                fillColor.argb,
                ClientRenderPipelines.GUI.triangles(cull),
                copyPose(),
                this.scissorStack.peek(),
                bounds,
            )
        )
    }

    if (outlineColor != null && !outlineColor.isTransparent) {
        drawLines(
            floatArrayOf(
                x0, y0,
                x1, y1,
                x1, y1,
                x2, y2,
                x2, y2,
                x0, y0,
            ),
            outlineColor.argb,
            bounds,
        )
    }
}

fun GuiGraphics.drawTriangle(
    p1: Vec2, p2: Vec2, p3: Vec2,
    fillColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
) = drawTriangle(
    p1.x, p1.y, p2.x, p2.y, p3.x, p3.y,
    fillColor, outlineColor,
)

@Suppress("LongParameterList")
inline fun GuiGraphics.drawGlyphOnCurrentLayer(
    textureSetup: TextureSetup,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    u1: Float = 0f,
    v1: Float = 0f,
    u2: Float = 1f,
    v2: Float = 1f,
    argb: Int = -1,
    pipeline: RenderPipeline = RenderPipelines.GUI_TEXTURED,
) {
    this.guiRenderState.submitGlyphToCurrentLayer(
        TexQuadGuiElementRenderState(
            x0,
            y0,
            x1,
            y1,
            u1,
            v1,
            u2,
            v2,
            argb,
            pipeline,
            textureSetup,
            copyPose(),
            this.scissorStack.peek(),
            null,
        )
    )
}

@Suppress("LongParameterList")
inline fun GuiGraphics.drawTexQuad(
    textureSetup: TextureSetup,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    u1: Float = 0f,
    v1: Float = 0f,
    u2: Float = 1f,
    v2: Float = 1f,
    argb: Int = -1,
    pipeline: RenderPipeline = RenderPipelines.GUI_TEXTURED,
) {
    this.guiRenderState.submitGuiElement(
        TexQuadGuiElementRenderState(
            x0,
            y0,
            x1,
            y1,
            u1,
            v1,
            u2,
            v2,
            argb,
            pipeline,
            textureSetup,
            copyPose(),
            this.scissorStack.peek(),
            createBounds(x0, y0, x1 - x0, y1 - y0),
        )
    )
}

@Suppress("LongParameterList")
inline fun GuiGraphics.drawBlitOnCurrentLayer(
    textureSetup: TextureSetup,
    x0: Int,
    y0: Int,
    x1: Int,
    y1: Int,
    u1: Float = 0f,
    v1: Float = 0f,
    u2: Float = 1f,
    v2: Float = 1f,
    argb: Int = -1,
    pipeline: RenderPipeline = RenderPipelines.GUI_TEXTURED,
) {
    this.guiRenderState.submitBlitToCurrentLayer(
        BlitRenderState(
            pipeline,
            textureSetup,
            copyPose(),
            x0,
            y0,
            x1,
            y1,
            u1,
            v1,
            u2,
            v2,
            argb,
            this.scissorStack.peek(),
            null,
        )
    )
}

fun GuiGraphics.drawCircle(
    x: Float,
    y: Float,
    radius: Float,
    innerRadius: Float = 0f,
    segments: Int = 40,
    colorGetter: Float2IntFunction = Float2IntFunction { Color4b.WHITE.argb },
) {
    val bounds = createBounds(x - radius, y - radius, radius * 2, radius * 2)

    this.guiRenderState.submitGuiElement(
        CircleGuiElementRenderState(
            x,
            y,
            radius,
            innerRadius,
            segments,
            colorGetter,
            ClientRenderPipelines.GUI.triangles(true),
            copyPose(),
            this.scissorStack.peek(),
            bounds
        )
    )
}
