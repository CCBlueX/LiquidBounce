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

import com.mojang.renderpearl.api.pipeline.RenderPipeline
import it.unimi.dsi.fastutil.floats.Float2IntFunction
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.GuiCircleLutAtlas
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.ceilToInt
import net.ccbluex.liquidbounce.utils.math.floorToInt
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.ccbluex.liquidbounce.render.gui.element.CircleGuiElementRenderState
import net.ccbluex.liquidbounce.render.gui.element.LambdaSimpleGuiElementRenderState
import net.ccbluex.liquidbounce.render.gui.element.LineGuiElementRenderState
import net.ccbluex.liquidbounce.render.gui.element.QuadGuiElementRenderState
import net.ccbluex.liquidbounce.render.gui.element.RoundedRectGuiElementRenderState
import net.ccbluex.liquidbounce.render.gui.element.TexQuadGuiElementRenderState
import net.ccbluex.liquidbounce.render.gui.element.TriangleGuiElementRenderState
import net.ccbluex.liquidbounce.utils.render.VerticesSetupHandler
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.BlitRenderState
import net.minecraft.world.phys.Vec2
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fStack
import org.joml.Matrix3x2fc
import org.joml.Vector2f
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.abs
import kotlin.math.sqrt

private val LEFT_TOP = Vector2f()
private val RIGHT_TOP = Vector2f()
private val LEFT_BOTTOM = Vector2f()
private val RIGHT_BOTTOM = Vector2f()

/**
 * Primitive version of [ScreenRectangle.transformMaxBounds]
 */
@PublishedApi
internal fun Matrix3x2fc.transformMaxBounds(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
): ScreenRectangle {
    val v1 = transformPosition(left, top, LEFT_TOP)
    val v2 = transformPosition(right, top, RIGHT_TOP)
    val v3 = transformPosition(left, bottom, LEFT_BOTTOM)
    val v4 = transformPosition(right, bottom, RIGHT_BOTTOM)
    val minX = minOf(v1.x, minOf(v3.x, v2.x, v4.x))
    val maxX = maxOf(v1.x, maxOf(v3.x, v2.x, v4.x))
    val minY = minOf(v1.y, minOf(v3.y, v2.y, v4.y))
    val maxY = maxOf(v1.y, maxOf(v3.y, v2.y, v4.y))
    return ScreenRectangle(
        minX.floorToInt(),
        minY.floorToInt(),
        (maxX - minX).ceilToInt(),
        (maxY - minY).ceilToInt(),
    )
}

val ScreenRectangle.isEmpty: Boolean
    get() = width == 0 || height == 0

@OptIn(ExperimentalContracts::class)
inline fun ScreenRectangle.ifEmpty(defaultValue: () -> ScreenRectangle): ScreenRectangle {
    contract {
        callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE)
    }
    return if (isEmpty) defaultValue() else this
}

/**
 * @see net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState.getBounds
 */
fun GuiGraphicsExtractor.getBounds(left: Float, top: Float, right: Float, bottom: Float): ScreenRectangle {
    val rect = this.pose().transformMaxBounds(left, top, right, bottom)
    return this.scissorStack.peek()?.intersection(rect) ?: rect
}

/**
 * @see net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState.getBounds
 */
fun GuiGraphicsExtractor.getBoundsXYWH(x: Float, y: Float, w: Float, h: Float): ScreenRectangle {
    return getBounds(x, y, x + w, y + h)
}

fun GuiGraphicsExtractor.getBounds(box: BoundingBox2f): ScreenRectangle =
    getBounds(box.xMin, box.yMin, box.xMax, box.yMax)

inline fun GuiGraphicsExtractor.copyPosePooled(): Matrix3x2f = Pools.Mat3x2f.borrow().set(this.pose())

inline fun GuiGraphicsExtractor.copyPose(): Matrix3x2f = Matrix3x2f(this.pose())

inline fun Matrix3x2fStack.withPush(block: Matrix3x2fStack.() -> Unit) {
    pushMatrix()
    try {
        block()
    } finally {
        popMatrix()
    }
}

inline fun GuiGraphicsExtractor.ScissorStack.withPush(
    rect: ScreenRectangle,
    block: GuiGraphicsExtractor.ScissorStack.() -> Unit,
) {
    if (rect.width <= 0 || rect.height <= 0) return
    push(rect)
    try {
        block()
    } finally {
        pop()
    }
}

/**
 * Runs [block] clipped to the rectangle from ([left], [top]) to ([right], [bottom]) in the current pose,
 * or not at all when the rectangle covers no pixel.
 */
inline fun GuiGraphicsExtractor.withScissor(left: Float, top: Float, right: Float, bottom: Float, block: () -> Unit) {
    scissorStack.withPush(pose().transformMaxBounds(left, top, right, bottom)) { block() }
}

inline fun GuiGraphicsExtractor.drawCustomElement(
    pipeline: RenderPipeline = RenderPipelines.GUI, // PosColor + QUADS
    textureSetup: TextureSetup = TextureSetup.noTexture(),
    scissorArea: ScreenRectangle? = this.scissorStack.peek(),
    bounds: ScreenRectangle? = null,
    verticesSetupHandler: VerticesSetupHandler,
) = this.guiRenderState.addGuiElement(
    LambdaSimpleGuiElementRenderState(
        pipeline,
        textureSetup,
        copyPosePooled(),
        scissorArea,
        bounds,
        verticesSetupHandler
    )
)

fun GuiGraphicsExtractor.drawLines(
    points: FloatArray,
    argb: Int,
    bounds: ScreenRectangle,
    cull: Boolean = true,
) {
    this.guiRenderState.addGuiElement(
        LineGuiElementRenderState(
            points,
            argb,
            ClientRenderPipelines.GUI.lines(cull),
            copyPosePooled(),
            this.scissorStack.peek(),
            bounds,
        )
    )
}

@AddonApi
@JvmOverloads
fun GuiGraphicsExtractor.drawQuad(
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

    val bounds = getBounds(x11, y11, x21, y21)

    if (fillColor != null && !fillColor.isTransparent) {
        this.guiRenderState.addGuiElement(
            QuadGuiElementRenderState(
                x11,
                y11,
                x21,
                y21,
                fillColor.argb,
                copyPosePooled(),
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
 * @param fillBottomColor Color at the bottom edge of the [fillColor], interpolated over the height. `null` keeps
 * the fill solid.
 */
@Suppress("LongParameterList")
fun GuiGraphicsExtractor.drawRoundedRect(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    radius: Float,
    fillColor: Color4b? = Color4b.TRANSPARENT,
    fillBottomColor: Color4b? = null,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
    outlineWidth: Float = 1.0f,
) {
    val x11 = minOf(x1, x2)
    val y11 = minOf(y1, y2)
    val x21 = maxOf(x1, x2)
    val y21 = maxOf(y1, y2)

    val fill = fillColor ?: Color4b.TRANSPARENT
    val outline = outlineColor ?: Color4b.TRANSPARENT
    if (fill.isTransparent && (outline.isTransparent || outlineWidth <= 0.0f)) {
        return
    }

    this.guiRenderState.addGuiElement(
        RoundedRectGuiElementRenderState(
            x11,
            y11,
            x21,
            y21,
            radius.coerceAtLeast(0.0f),
            fill.argb,
            (fillBottomColor ?: fill).argb,
            outline.argb,
            outlineWidth.coerceAtLeast(0.0f),
            copyPosePooled(),
            this.scissorStack.peek(),
            getBounds(x11, y11, x21, y21),
        )
    )
}

inline fun GuiGraphicsExtractor.drawQuadXYWH(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    fillColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
) = drawQuad(x, y, x + w, y + h, fillColor, outlineColor)

/**
 * Float version of [GuiGraphicsExtractor.drawHorizontalLine]
 */
fun GuiGraphicsExtractor.drawHorizontalLine(x1: Float, x2: Float, y: Float, thickness: Float, color: Color4b) {
    this.drawQuad(x1, y, x2, y + thickness, color)
}

/**
 * Float version of [GuiGraphicsExtractor.drawVerticalLine]
 */
fun GuiGraphicsExtractor.drawVerticalLine(x: Float, y1: Float, y2: Float, thickness: Float, color: Color4b) {
    this.drawQuad(x, y1, x + thickness, y2, color)
}

@Suppress("LongParameterList")
fun GuiGraphicsExtractor.drawTriangle(
    x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float,
    fillColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
    cull: Boolean = true,
) {
    val minX = minOf(x0, x1, x2)
    val minY = minOf(y0, y1, y2)
    val maxX = maxOf(x0, x1, x2)
    val maxY = maxOf(y0, y1, y2)
    val bounds = getBounds(minX, minY, maxX, maxY)

    if (fillColor != null && !fillColor.isTransparent) {
        this.guiRenderState.addGuiElement(
            TriangleGuiElementRenderState(
                x0, y0, x1, y1, x2, y2,
                fillColor.argb,
                ClientRenderPipelines.GUI.triangles(cull),
                copyPosePooled(),
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

fun GuiGraphicsExtractor.drawTriangle(
    p1: Vec2, p2: Vec2, p3: Vec2,
    fillColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
) = drawTriangle(
    p1.x, p1.y, p2.x, p2.y, p3.x, p3.y,
    fillColor, outlineColor,
)

@Suppress("LongParameterList")
inline fun GuiGraphicsExtractor.drawTexQuad(
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
    this.guiRenderState.addGuiElement(
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
            copyPosePooled(),
            this.scissorStack.peek(),
            getBounds(x0, y0, x1, y1),
        )
    )
}

@Suppress("LongParameterList")
inline fun GuiGraphicsExtractor.drawBlitOnCurrentLayer(
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
    this.guiRenderState.addBlitToCurrentLayer(
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

fun GuiGraphicsExtractor.drawCircle(
    x: Float,
    y: Float,
    radius: Float,
    innerRadius: Float = 0f,
    colorGetter: Float2IntFunction = Float2IntFunction { Color4b.WHITE.argb },
) {
    if (radius <= 0f) {
        return
    }

    val lut = GuiCircleLutAtlas.allocate(colorGetter)
    val innerRatio = (innerRadius / radius).coerceIn(0f, 1f)
    val bounds = getBoundsXYWH(x - radius, y - radius, radius * 2, radius * 2)

    this.guiRenderState.addGuiElement(
        CircleGuiElementRenderState(
            x,
            y,
            radius,
            innerRatio,
            lut.row,
            ClientRenderPipelines.GUI.circleLut(),
            lut.textureSetup,
            copyPosePooled(),
            this.scissorStack.peek(),
            bounds
        )
    )
}

/**
 * Fills the convex polygon of the first [count] points (x, y pairs) of [points] around ([centerX], [centerY]),
 * with edges faded over one framebuffer pixel. Uses only vanilla shaders, so it also works during the first
 * loading overlay.
 */
fun GuiGraphicsExtractor.drawConvexPolygon(
    centerX: Float,
    centerY: Float,
    points: FloatArray,
    count: Int,
    argb: Int,
) {
    // Per corner (inner x, inner y, outer x, outer y); the element keeps it until the GUI draws
    val ring = FloatArray(count * 4)
    val corners = distinctCorners(points, count, ring)
    if (corners < 3) {
        return
    }

    // Half a framebuffer pixel in the polygon's own units
    val transform = pose()
    val scale = sqrt(abs(transform.m00() * transform.m11() - transform.m01() * transform.m10())) * mc.window.guiScale
    val half = if (scale > 0f) 0.5f / scale else 0f

    putNormals(ring, corners, centerX, centerY)

    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (corner in 0 until corners) {
        val index = corner * 4
        val x = ring[index]
        val y = ring[index + 1]
        val normalX = ring[index + 2] * half
        val normalY = ring[index + 3] * half
        ring[index] = x - normalX
        ring[index + 1] = y - normalY
        ring[index + 2] = x + normalX
        ring[index + 3] = y + normalY
        minX = minOf(minX, x + normalX)
        minY = minOf(minY, y + normalY)
        maxX = maxOf(maxX, x + normalX)
        maxY = maxOf(maxY, y + normalY)
    }

    val clear = argb and 0x00FFFFFF
    drawCustomElement(ClientRenderPipelines.GUI.triangles(false), bounds = getBounds(minX, minY, maxX, maxY)) { pose ->
        for (corner in 0 until corners) {
            val a = corner * 4
            val b = (corner + 1) % corners * 4
            addVertexWith2DPose(pose, centerX, centerY).setColor(argb)
            addVertexWith2DPose(pose, ring[a], ring[a + 1]).setColor(argb)
            addVertexWith2DPose(pose, ring[b], ring[b + 1]).setColor(argb)

            addVertexWith2DPose(pose, ring[a], ring[a + 1]).setColor(argb)
            addVertexWith2DPose(pose, ring[a + 2], ring[a + 3]).setColor(clear)
            addVertexWith2DPose(pose, ring[b + 2], ring[b + 3]).setColor(clear)
            addVertexWith2DPose(pose, ring[a], ring[a + 1]).setColor(argb)
            addVertexWith2DPose(pose, ring[b + 2], ring[b + 3]).setColor(clear)
            addVertexWith2DPose(pose, ring[b], ring[b + 1]).setColor(argb)
        }
    }
}

/**
 * Copies the first [count] points of [points] into the position slots of [ring] (4 floats per corner), leaving out
 * points that repeat the one before, as the corners of a rect without rounding do; returns the number of corners.
 */
private fun distinctCorners(points: FloatArray, count: Int, ring: FloatArray): Int {
    var corners = 0
    for (index in 0 until count) {
        val x = points[index * 2]
        val y = points[index * 2 + 1]
        if (corners == 0 || !ring.samePoint((corners - 1) * 4, x, y)) {
            ring[corners * 4] = x
            ring[corners * 4 + 1] = y
            corners++
        }
    }
    if (corners > 1 && ring.samePoint(0, ring[(corners - 1) * 4], ring[(corners - 1) * 4 + 1])) {
        corners--
    }
    return corners
}

/**
 * Puts each corner's outward bisector normal, away from ([centerX], [centerY]), into its outer slots of [ring],
 * while the neighbours' positions are still in place.
 */
private fun putNormals(ring: FloatArray, corners: Int, centerX: Float, centerY: Float) {
    for (corner in 0 until corners) {
        val prev = (corner + corners - 1) % corners * 4
        val next = (corner + 1) % corners * 4
        var normalX = ring[next + 1] - ring[prev + 1]
        var normalY = ring[prev] - ring[next]
        val length = sqrt(normalX * normalX + normalY * normalY)
        if (length > 0f) {
            normalX /= length
            normalY /= length
        }
        if (normalX * (ring[corner * 4] - centerX) + normalY * (ring[corner * 4 + 1] - centerY) < 0f) {
            normalX = -normalX
            normalY = -normalY
        }
        ring[corner * 4 + 2] = normalX
        ring[corner * 4 + 3] = normalY
    }
}

private fun FloatArray.samePoint(index: Int, x: Float, y: Float) =
    abs(this[index] - x) < EPSILON && abs(this[index + 1] - y) < EPSILON

private const val EPSILON = 1e-4f
