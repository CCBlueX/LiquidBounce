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
package net.ccbluex.liquidbounce.render.splash

import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.drawConvexPolygon
import net.ccbluex.liquidbounce.render.drawTexQuad
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.render.withScissor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.util.ARGB
import org.joml.Matrix3x2f
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws frames of a [LottieAnimation] in the composition space set up by the current pose.
 *
 * Two layer names are hooks for the client: [PROGRESS] is stretched horizontally around its anchor by
 * the loading progress, and the anchor of [CONTENT] is where the client places its own text.
 */
class LottieRenderer(
    private val animation: LottieAnimation,
    private val textures: Map<String, TextureSetup>,
) {

    var progress = 0f

    /** GUI position of the [CONTENT] layer's anchor after the last [render]; NaN without one. */
    val content = Vector2f(Float.NaN, Float.NaN)

    private val images = animation.images.associateBy(LottieImage::id)
    private val local = Matrix3x2f()
    private val shape = Matrix3x2f()
    private val toGui = Matrix3x2f()
    private val maskBounds = Vector4f()
    private val polygon = FloatArray(maxOf(4 * (CORNER_SEGMENTS + 1), ELLIPSE_SEGMENTS + 1) * 2)
    private var stamp = 0

    fun render(graphics: GuiGraphicsExtractor, frame: Float, alpha: Float) {
        content.set(Float.NaN, Float.NaN)
        renderComposition(graphics, animation.layers, frame, alpha)
    }

    private fun renderComposition(
        graphics: GuiGraphicsExtractor,
        layers: List<LottieLayer>,
        frame: Float,
        alpha: Float,
    ) {
        stamp++
        for (layer in layers) {
            world(layer, layers, frame)
            if (layer.name == CONTENT) {
                toGui.set(graphics.pose()).mul(layer.world)
                    .transformPosition(layer.transform.anchorX(frame), layer.transform.anchorY(frame), content)
            }
        }

        // The first layer is the top one
        for (index in layers.indices.reversed()) {
            val layer = layers[index]
            if (!layer.isVisibleAt(frame)) {
                continue
            }

            val layerAlpha = alpha * layer.transform.opacity(frame)
            if (layerAlpha <= MIN_ALPHA) {
                continue
            }

            if (layer.name == PROGRESS) {
                drawLayer(graphics, layer, frame, layerAlpha, progress.coerceIn(0f, 1f))
            } else {
                drawLayer(graphics, layer, frame, layerAlpha, 1f)
            }
        }
    }

    /**
     * The layer's matrix in its composition: its parents' matrices times its own. Parents pass on their
     * transform but not their opacity, as in Lottie.
     */
    private fun world(layer: LottieLayer, layers: List<LottieLayer>, frame: Float): Matrix3x2f {
        if (layer.worldStamp == stamp) {
            return layer.world
        }
        layer.worldStamp = stamp

        val parent = layer.parent?.let { index -> layers.find { other -> other.index == index && other !== layer } }
        val parentWorld = parent?.let { world(it, layers, frame) }
        layer.transform.matrix(frame, local)
        if (parentWorld != null) {
            layer.world.set(parentWorld).mul(local)
        } else {
            layer.world.set(local)
        }
        return layer.world
    }

    private fun drawLayer(
        graphics: GuiGraphicsExtractor,
        layer: LottieLayer,
        frame: Float,
        alpha: Float,
        scaleX: Float,
    ) {
        graphics.pose().withPush {
            mul(layer.world)
            if (scaleX != 1f) {
                val anchorX = layer.transform.anchorX(frame)
                translate(anchorX, 0f).scale(scaleX, 1f).translate(-anchorX, 0f)
            }

            if (computeMaskBounds(layer, frame)) {
                // Masks are rectangles here, so their bounds are the clip; a closed one hides the layer
                graphics.withScissor(maskBounds.x, maskBounds.y, maskBounds.z, maskBounds.w) {
                    drawContent(graphics, layer, frame, alpha)
                }
            } else {
                drawContent(graphics, layer, frame, alpha)
            }
        }
    }

    private fun drawContent(graphics: GuiGraphicsExtractor, layer: LottieLayer, frame: Float, alpha: Float) {
        when (val content = layer.content) {
            is LottieContent.Image -> {
                val image = images[content.imageId]
                val texture = textures[content.imageId]
                if (image != null && texture != null) {
                    graphics.drawTexQuad(
                        texture,
                        0f,
                        0f,
                        image.width.toFloat(),
                        image.height.toFloat(),
                        argb = ARGB.white(alpha),
                        pipeline = ClientRenderPipelines.GUI.SmoothTexture,
                    )
                }
            }

            is LottieContent.Solid -> graphics.fill(
                0, 0, content.width.toInt(), content.height.toInt(), ARGB.color(alpha, content.color)
            )

            is LottieContent.Precomp ->
                renderComposition(graphics, content.layers, (frame - layer.startTime) / layer.timeStretch, alpha)

            is LottieContent.Shapes -> drawShapes(graphics, content.items, frame, alpha)
            LottieContent.Empty -> {}
        }
    }

    /**
     * Puts the bounds of the layer's masks into [maskBounds] as (minX, minY, maxX, maxY); false without masks.
     */
    private fun computeMaskBounds(layer: LottieLayer, frame: Float): Boolean {
        if (layer.masks.isEmpty()) {
            return false
        }

        val bounds = maskBounds.set(
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
        )
        for (mask in layer.masks) {
            for (vertex in 0 until mask.dimensions / 2) {
                val x = mask.value(frame, vertex * 2)
                val y = mask.value(frame, vertex * 2 + 1)
                bounds.set(minOf(bounds.x, x), minOf(bounds.y, y), maxOf(bounds.z, x), maxOf(bounds.w, y))
            }
        }
        return bounds.x <= bounds.z
    }

    /**
     * Draws the rectangles and ellipses of a group with the group's fill; the first item is the top one.
     */
    private fun drawShapes(graphics: GuiGraphicsExtractor, items: List<LottieShape>, frame: Float, alpha: Float) {
        val fill = items.firstOrNull { item -> item is LottieShape.Fill } as LottieShape.Fill?
        val color = if (fill != null) fillColor(fill, frame, alpha) else 0
        val filled = ARGB.alpha(color) > 0

        for (index in items.indices.reversed()) {
            when (val item = items[index]) {
                is LottieShape.Group -> {
                    val groupAlpha = alpha * item.transform.opacity(frame)
                    if (groupAlpha > MIN_ALPHA) {
                        graphics.pose().withPush {
                            mul(item.transform.matrix(frame, shape))
                            drawShapes(graphics, item.items, frame, groupAlpha)
                        }
                    }
                }

                is LottieShape.Rect -> if (filled) drawRect(graphics, item, frame, color)
                is LottieShape.Ellipse -> if (filled) drawEllipse(graphics, item, frame, color)
                is LottieShape.Fill -> {}
            }
        }
    }

    private fun fillColor(fill: LottieShape.Fill, frame: Float, alpha: Float) = ARGB.colorFromFloat(
        (alpha * fill.opacity.value(frame) / 100f).coerceIn(0f, 1f),
        fill.color.value(frame, 0).coerceIn(0f, 1f),
        fill.color.value(frame, 1).coerceIn(0f, 1f),
        fill.color.value(frame, 2).coerceIn(0f, 1f),
    )

    private fun drawRect(graphics: GuiGraphicsExtractor, rect: LottieShape.Rect, frame: Float, color: Int) {
        val x = rect.position.value(frame, 0)
        val y = rect.position.value(frame, 1)
        val halfWidth = rect.size.value(frame, 0) / 2f
        val halfHeight = rect.size.value(frame, 1) / 2f
        if (halfWidth <= 0f || halfHeight <= 0f) {
            return
        }

        val radius = rect.roundness.value(frame).coerceIn(0f, minOf(halfWidth, halfHeight))
        var offset = 0
        for (corner in 0 until 4) {
            // Clockwise from the top right corner, in screen coordinates where y points down
            val centerX = x + CORNER_X[corner] * (halfWidth - radius)
            val centerY = y + CORNER_Y[corner] * (halfHeight - radius)
            val start = (corner - 1) * HALF_PI
            offset = arc(polygon, offset, centerX, centerY, radius, radius, start, HALF_PI, CORNER_SEGMENTS)
        }
        graphics.drawConvexPolygon(x, y, polygon, offset / 2, color)
    }

    private fun drawEllipse(graphics: GuiGraphicsExtractor, ellipse: LottieShape.Ellipse, frame: Float, color: Int) {
        val width = ellipse.size.value(frame, 0)
        val height = ellipse.size.value(frame, 1)
        if (width <= 0f || height <= 0f) {
            return
        }

        val x = ellipse.position.value(frame, 0)
        val y = ellipse.position.value(frame, 1)
        val count = arc(polygon, 0, x, y, width / 2f, height / 2f, 0f, TWO_PI, ELLIPSE_SEGMENTS) / 2
        graphics.drawConvexPolygon(x, y, polygon, count, color)
    }

    companion object {
        const val PROGRESS = "progress"
        const val CONTENT = "content"
        private const val MIN_ALPHA = 0.002f
        private const val CORNER_SEGMENTS = 6
        private const val ELLIPSE_SEGMENTS = 48
        private const val HALF_PI = (PI / 2).toFloat()
        private const val TWO_PI = (PI * 2).toFloat()
        private val CORNER_X = floatArrayOf(1f, 1f, -1f, -1f)
        private val CORNER_Y = floatArrayOf(-1f, 1f, 1f, -1f)
    }

}

/**
 * Writes [segments] + 1 points of an elliptic arc into [out] from [offset]; returns the next offset.
 */
@Suppress("LongParameterList")
private fun arc(
    out: FloatArray,
    offset: Int,
    centerX: Float,
    centerY: Float,
    radiusX: Float,
    radiusY: Float,
    start: Float,
    sweep: Float,
    segments: Int,
): Int {
    var index = offset
    for (step in 0..segments) {
        val angle = start + sweep * step / segments
        out[index++] = centerX + radiusX * cos(angle)
        out[index++] = centerY + radiusY * sin(angle)
    }
    return index
}
