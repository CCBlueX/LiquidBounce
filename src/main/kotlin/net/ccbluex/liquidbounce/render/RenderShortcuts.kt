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
@file:Suppress("detekt:TooManyFunctions")

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinDrawContextAccessor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.UV2f
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.enumMapOf
import net.ccbluex.liquidbounce.utils.kotlin.unmodifiable
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.*
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import org.joml.Matrix3x2fStack
import org.joml.Matrix4f
import org.joml.Vector3fc
import org.lwjgl.opengl.GL11C
import java.util.EnumMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.use

/**
 * This variable should be used when rendering long lines, meaning longer than ~2 in 3d.
 * [WorldRenderEnvironment.longLines] is available for this.
 *
 * Context:
 * For some reason, newer drivers for AMD Vega iGPUs (about end 2023 until now) fail to correctly smooth lines.
 *
 * This has to be removed or limited to old driver versions when AMD actually fixes the bug in their drivers.
 * But as of now, 01.02.2025, they haven't.
 */
@JvmField
val HAS_AMD_VEGA_APU = GL11C.glGetString(GL11C.GL_RENDERER)?.startsWith("AMD Radeon(TM) RX Vega") ?: false &&
    GL11C.glGetString(GL11C.GL_VENDOR) == "ATI Technologies Inc."

@JvmField
val FULL_BOX = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

@JvmField
val EMPTY_BOX = Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

/**
 * Data class representing the rendering environment.
 *
 * @property matrixStack The matrix stack for rendering.
 */
sealed class RenderEnvironment(val matrixStack: MatrixStack) {
    var isBatchMode: Boolean = false
        private set

    fun getOrCreateBuffer(drawMode: DrawMode, vertexInputType: VertexInputType): BufferBuilder {
        return if (isBatchMode) {
            batchBuffer[vertexInputType]!!.getOrPut(drawMode) {
                ClientTessellator.begin(drawMode, vertexInputType)
            }
        } else {
            Tessellator.getInstance().begin(drawMode, vertexInputType.vertexFormat)
        }
    }

    fun startBatch() {
        if (isBatchMode) commitBatch()
        isBatchMode = true
    }

    fun commitBatch() {
        require(isBatchMode) {
            "Current environment is not in batch mode!"
        }

        batchBuffer.forEach { (vertexInputType, map) ->
            map.forEach { (drawMode, bufferBuilder) ->
                bufferBuilder.endNullable()?.let {
                    it.draw(vertexInputType)
                    ClientTessellator.allocator(drawMode, vertexInputType).clear()
                }
            }
            map.clear()
        }
    }

    open fun relativeToCamera(pos: Vec3d): Vec3d = pos

    companion object {
        @JvmStatic
        private val batchBuffer = enumMapOf<VertexInputType, EnumMap<DrawMode, BufferBuilder>> { _ ->
            enumMapOf()
        }
    }
}

class GUIRenderEnvironment(
    val context: DrawContext,
    matrixStack: MatrixStack?,
) : RenderEnvironment(matrixStack ?: context.matrices)

class WorldRenderEnvironment(
    matrixStack: MatrixStack,
    val camera: Camera,
) : RenderEnvironment(matrixStack) {
    override fun relativeToCamera(pos: Vec3d): Vec3d {
        return pos.subtract(camera.pos)
    }

    fun relativeToCamera(pos: Vec3i): Vec3d {
        return Vec3d(pos.x.toDouble() - camera.pos.x, pos.y.toDouble() - camera.pos.y, pos.z.toDouble() - camera.pos.z)
    }
}

/**
 * Helper function to render an environment with the specified [matrixStack] and [draw] block.
 *
 * @param matrixStack The matrix stack for rendering.
 * @param draw The block of code to be executed in the rendering environment.
 */
@OptIn(ExperimentalContracts::class)
inline fun renderEnvironmentForWorld(matrixStack: MatrixStack, draw: WorldRenderEnvironment.() -> Unit) {
    contract {
        callsInPlace(draw, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }

    val camera = mc.entityRenderDispatcher.camera ?: return

    RenderSystem.enableBlend()
    RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)
    RenderSystem.disableDepthTest()
    GL11C.glEnable(GL11C.GL_LINE_SMOOTH)

    val environment = WorldRenderEnvironment(matrixStack, camera)
    draw(environment)
    if (environment.isBatchMode) environment.commitBatch()

    RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    RenderSystem.disableBlend()
    RenderSystem.enableDepthTest()
    RenderSystem.enableCull()
    GL11C.glDisable(GL11C.GL_LINE_SMOOTH)
}

@OptIn(ExperimentalContracts::class)
inline fun renderEnvironmentForGUI(
    event: OverlayRenderEvent,
    matrixStack: MatrixStack? = null,
    draw: GUIRenderEnvironment.() -> Unit
) {
    contract {
        callsInPlace(draw, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }

    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    RenderSystem.enableBlend()

    val environment = GUIRenderEnvironment(event.context, matrixStack)
    draw(environment)
    if (environment.isBatchMode) environment.commitBatch()

    RenderSystem.disableBlend()
}

inline fun MatrixStack.withPush(block: MatrixStack.() -> Unit) {
    push()
    try {
        block()
    } finally {
        pop()
    }
}

inline fun Matrix3x2fStack.withPush(block: Matrix3x2fStack.() -> Unit) {
    pushMatrix()
    try {
        block()
    } finally {
        popMatrix()
    }
}

/**
 * Extension function to apply a position transformation to the current rendering environment.
 *
 * @param pos The position vector.
 * @param draw The block of code to be executed in the transformed environment.
 */
inline fun RenderEnvironment.withPosition(pos: Vec3, draw: RenderEnvironment.() -> Unit) {
    matrixStack.withPush {
        translate(pos.x, pos.y, pos.z)
        draw()
    }
}

/**
 * Extension function to apply a position transformation to the current rendering environment.
 *
 * @param pos The position vector.
 * @param draw The block of code to be executed in the transformed environment.
 */
inline fun <T : RenderEnvironment> T.withPosition(pos: Vec3d, draw: T.() -> Unit) {
    matrixStack.withPush {
        translate(pos.x, pos.y, pos.z)
        draw()
    }
}

/**
 * Shorthand for `withPosition(relativeToCamera(pos))`
 */
inline fun WorldRenderEnvironment.withPositionRelativeToCamera(pos: Vec3d, draw: WorldRenderEnvironment.() -> Unit) {
    withPosition(relativeToCamera(pos), draw)
}

/**
 * Shortcut of `withPositionRelativeToCamera(Vec3d.of(pos))`
 */
inline fun WorldRenderEnvironment.withPositionRelativeToCamera(pos: Vec3i, draw: WorldRenderEnvironment.() -> Unit) {
    val relativePos = relativeToCamera(pos)

    matrixStack.withPush {
        translate(relativePos.x, relativePos.y, relativePos.z)
        draw()
    }
}


/**
 * Disables [GL11C.GL_LINE_SMOOTH] if [HAS_AMD_VEGA_APU].
 */
inline fun WorldRenderEnvironment.longLines(draw: RenderEnvironment.() -> Unit) {
    if (!HAS_AMD_VEGA_APU) {
        draw()
        return
    }

    GL11C.glDisable(GL11C.GL_LINE_SMOOTH)
    try {
        draw()
    } finally {
        GL11C.glEnable(GL11C.GL_LINE_SMOOTH)
    }
}

/**
 * Extension function to disable cull
 * Good for rendering faces that should be visible from both sides
 *
 * @param draw The block of code to be executed with cull disabled.
 */
inline fun RenderEnvironment.withDisabledCull(draw: RenderEnvironment.() -> Unit) {
    RenderSystem.disableCull()
    try {
        draw()
    } finally {
        RenderSystem.enableCull()
    }
}

inline fun RenderEnvironment.drawCustomMesh(
    drawMode: DrawMode,
    vertexInputType: VertexInputType,
    drawer: VertexConsumer.(Matrix4f) -> Unit
) {
    val matrix = matrixStack.peek().positionMatrix

    val buffer = getOrCreateBuffer(drawMode, vertexInputType)

    drawer(buffer, matrix)

    if (!isBatchMode) {
        buffer.endNullable()?.draw(vertexInputType)
    }
}

fun BuiltBuffer.draw(vertexInputType: VertexInputType) = use { builtBuffer ->
    RenderSystem.setShader(vertexInputType.shaderProgram)
    BufferRenderer.drawWithGlobalProgram(builtBuffer)
}

/**
 * Function to draw lines using the specified [lines] vectors.
 *
 * @param lines The vectors representing the lines.
 */
fun RenderEnvironment.drawLines(argb: Int, vararg lines: Vec3) {
    drawLines(
        lines.unmodifiable(),
        mode = DrawMode.DEBUG_LINES,
        argb = argb,
    )
}

/**
 * Function to draw a line strip using the specified [positions] vectors.
 *
 * @param positions The vectors representing the line strip.
 */
fun RenderEnvironment.drawLineStrip(argb: Int, vararg positions: Vec3) {
    drawLines(
        positions.unmodifiable(),
        mode = DrawMode.DEBUG_LINE_STRIP,
        argb = argb,
    )
}

/**
 * Helper function to draw lines using the specified [lines] vectors and draw mode.
 *
 * @param lines The vectors representing the lines.
 * @param mode The draw mode for the lines.
 */
private fun RenderEnvironment.drawLines(
    lines: List<Vec3>,
    mode: DrawMode,
    argb: Int,
) {
    // If the array of lines is empty, we don't need to draw anything
    if (lines.isEmpty()) {
        return
    }

    drawCustomMesh(
        mode,
        VertexInputType.PosColor,
    ) { matrix ->
        lines.forEach { (x, y, z) ->
            vertex(matrix, x, y, z).color(argb)
        }
    }
}

fun RenderEnvironment.drawSquareTexture(
    size: Float,
    argb: Int,
) = drawCustomMesh(
    DrawMode.QUADS,
    VertexInputType.PosTexColor,
) { matrix ->
    vertex(matrix, 0.0f, -size, 0.0f)
        .texture(0.0f, 0.0f)
        .color(argb)

    vertex(matrix, -size, -size, 0.0f)
        .texture(0.0f, 1.0f)
        .color(argb)

    vertex(matrix, -size, 0.0f, 0.0f)
        .texture(1.0f, 1.0f)
        .color(argb)

    vertex(matrix, 0.0f, 0.0f, 0.0f)
        .texture(1.0f, 0.0f)
        .color(argb)
}

fun RenderEnvironment.drawTextureQuad(
    pos1: Vector3fc,
    uv1: UV2f = UV2f(0f, 0f),
    pos2: Vector3fc,
    uv2: UV2f = UV2f(1f, 1f),
    argb: Int,
) {
    drawCustomMesh(
        DrawMode.QUADS,
        VertexInputType.PosTexColor,
    ) { matrix ->
        vertex(matrix, pos1.x(), pos2.y(), pos1.z())
            .texture(uv1.u, uv2.v)
            .color(argb)
        vertex(matrix, pos2.x(), pos2.y(), pos2.z())
            .texture(uv2.u, uv2.v)
            .color(argb)
        vertex(matrix, pos2.x(), pos1.y(), pos2.z())
            .texture(uv2.u, uv1.v)
            .color(argb)
        vertex(matrix, pos1.x(), pos1.y(), pos1.z())
            .texture(uv1.u, uv1.v)
            .color(argb)
    }
}

fun RenderEnvironment.drawQuad(pos1: Vec3, pos2: Vec3, argb: Int) {
    drawCustomMesh(
        DrawMode.QUADS,
        VertexInputType.PosColor,
    ) { matrix ->
        vertex(matrix, pos1.x, pos2.y, pos1.z).color(argb)
        vertex(matrix, pos2.x, pos2.y, pos2.z).color(argb)
        vertex(matrix, pos2.x, pos1.y, pos2.z).color(argb)
        vertex(matrix, pos1.x, pos1.y, pos1.z).color(argb)
    }
}

fun RenderEnvironment.drawColoredQuad(pos1: Vec3, pos2: Vec3, argb: Int) {
    drawCustomMesh(
        DrawMode.QUADS,
        VertexInputType.PosColor,
    ) { matrix ->
        vertex(matrix, pos1.x, pos2.y, pos1.z).color(argb)
        vertex(matrix, pos2.x, pos2.y, pos2.z).color(argb)
        vertex(matrix, pos2.x, pos1.y, pos2.z).color(argb)
        vertex(matrix, pos1.x, pos1.y, pos1.z).color(argb)
    }
}

fun RenderEnvironment.drawQuadOutlines(pos1: Vec3, pos2: Vec3, argb: Int) {
    drawCustomMesh(
        DrawMode.DEBUG_LINES,
        VertexInputType.PosColor,
    ) { matrix ->
        vertex(matrix, pos1.x, pos1.y, pos1.z).color(argb)
        vertex(matrix, pos1.x, pos2.y, pos1.z).color(argb)

        vertex(matrix, pos1.x, pos2.y, pos1.z).color(argb)
        vertex(matrix, pos2.x, pos2.y, pos1.z).color(argb)

        vertex(matrix, pos2.x, pos1.y, pos1.z).color(argb)
        vertex(matrix, pos2.x, pos2.y, pos1.z).color(argb)

        vertex(matrix, pos1.x, pos1.y, pos1.z).color(argb)
        vertex(matrix, pos2.x, pos1.y, pos1.z).color(argb)
    }
}

fun RenderEnvironment.drawColoredQuadOutlines(pos1: Vec3, pos2: Vec3, argb: Int) {
    drawCustomMesh(
        DrawMode.DEBUG_LINES,
        VertexInputType.PosColor,
    ) { matrix ->
        vertex(matrix, pos1.x, pos1.y, pos1.z).color(argb)
        vertex(matrix, pos1.x, pos2.y, pos1.z).color(argb)

        vertex(matrix, pos1.x, pos2.y, pos1.z).color(argb)
        vertex(matrix, pos2.x, pos2.y, pos1.z).color(argb)

        vertex(matrix, pos2.x, pos1.y, pos1.z).color(argb)
        vertex(matrix, pos2.x, pos2.y, pos1.z).color(argb)

        vertex(matrix, pos1.x, pos1.y, pos1.z).color(argb)
        vertex(matrix, pos2.x, pos1.y, pos1.z).color(argb)
    }
}

fun RenderEnvironment.drawTriangle(p1: Vec3, p2: Vec3, p3: Vec3, argb: Int) {
    drawCustomMesh(
        DrawMode.TRIANGLES,
        VertexInputType.PosColor,
    ) { matrix ->
        vertex(matrix, p1.x, p1.y, p1.z).color(argb)
        vertex(matrix, p2.x, p2.y, p2.z).color(argb)
        vertex(matrix, p3.x, p3.y, p3.z).color(argb)
    }
}

fun VertexConsumer.coloredTriangle(matrix: Matrix4f, p1: Vec3d, p2: Vec3d, p3: Vec3d, color4b: Color4b) {
    vertex(matrix, p1.x.toFloat(), p1.y.toFloat(), p1.z.toFloat()).color(color4b.toARGB())
    vertex(matrix, p2.x.toFloat(), p2.y.toFloat(), p2.z.toFloat()).color(color4b.toARGB())
    vertex(matrix, p3.x.toFloat(), p3.y.toFloat(), p3.z.toFloat()).color(color4b.toARGB())
}


/**
 * Helper unction to draw a solid box using the specified [box].
 *
 * @param box The bounding box of the box.
 */
@Suppress("CognitiveComplexMethod")
private fun RenderEnvironment.drawBox(
    box: Box,
    drawMode: DrawMode,
    useOutlineVertices: Boolean = false,
    color: Color4b? = null,
    verticesToUse: Int = -1
) = drawCustomMesh(drawMode, VertexInputType.PosColor) { matrix ->
    val check = verticesToUse != -1

    // Draw the vertices of the box
    if (useOutlineVertices) {
        box.forEachOutlineVertex { i, x, y, z ->
            if (check && (verticesToUse and (1 shl i)) != 0) {
                return@forEachOutlineVertex
            }

            val bb = vertex(matrix, x.toFloat(), y.toFloat(), z.toFloat())

            if (color != null) {
                bb.color(color.toARGB())
            }
        }
    } else {
        box.forEachFaceVertex { i, x, y, z ->
            if (check && (verticesToUse and (1 shl i)) != 0) {
                return@forEachFaceVertex
            }

            val bb = vertex(matrix, x.toFloat(), y.toFloat(), z.toFloat())

            if (color != null) {
                bb.color(color.toARGB())
            }
        }
    }
}

/**
 * Function to draw a colored [box].
 */
fun RenderEnvironment.drawBox(
    box: Box,
    faceColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
    faceVertices: Int = -1,
    outlineVertices: Int = -1,
) {
    if (faceColor != null && !faceColor.isTransparent && faceVertices != 0) {
        drawBox(box, DrawMode.QUADS, color = faceColor, verticesToUse = faceVertices)
    }

    if (outlineColor != null && !outlineColor.isTransparent && outlineVertices != 0) {
        drawBox(box, DrawMode.DEBUG_LINES, useOutlineVertices = true, outlineColor, outlineVertices)
    }
}

/**
 * Function to draw a colored [box] with specified [side].
 */
fun RenderEnvironment.drawBoxSide(
    box: Box,
    side: Direction,
    faceColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
) = drawBox(
    box,
    faceColor,
    outlineColor,
    faceVertices = BoxVertexIterator.FACE.sideMask(side),
    outlineVertices = BoxVertexIterator.OUTLINE.sideMask(side),
)

/**
 * Function to render a gradient quad using specified [vertices] and [colors]
 *
 * @param vertices The four vectors to draw the quad
 * @param colors The colors for the vertices
 */
fun RenderEnvironment.drawGradientQuad(vertices: List<Vec3>, colors: List<Color4b>) {
    require(vertices.size == colors.size) { "there must be a color for every vertex" }
    require(vertices.size % 4 == 0) { "vertices must be dividable by 4" }
    drawCustomMesh(
        DrawMode.QUADS,
        VertexInputType.PosColor,
    ) { matrix ->
        vertices.forEachIndexed { index, (x, y, z) ->
            val color4b = colors[index]
            vertex(matrix, x, y, z).color(color4b.toARGB())
        }
    }
}

private const val CIRCLE_RES = 40

// using a val instead of a function for better performance
private val circlePoints = Array(CIRCLE_RES + 1) {
    val theta = MathHelper.PI * 2 * it / CIRCLE_RES
    Vec3(theta.fastCos(), 0f, theta.fastSin())
}

/**
 * Function to draw a circle of the size [outerRadius] with a cutout of size [innerRadius]
 *
 * @param outerRadius The radius of the circle
 * @param innerRadius The radius inside the circle (the cutout)
 * @param outerColor4b The color of the outer edges
 * @param innerColor4b The color of the inner edges
 */
fun RenderEnvironment.drawGradientCircle(
    outerRadius: Float,
    innerRadius: Float,
    outerColor4b: Color4b,
    innerColor4b: Color4b,
    innerOffset: Vec3 = Vec3.ZERO,
) {
    drawCustomMesh(
        DrawMode.TRIANGLE_STRIP,
        VertexInputType.PosColor,
    ) { matrix ->
        for (p in circlePoints) {
            val outerP = p * outerRadius
            val innerP = p * innerRadius + innerOffset

            vertex(matrix, outerP.x, outerP.y, outerP.z)
                .color(outerColor4b.toARGB())
            vertex(matrix, innerP.x, innerP.y, innerP.z)
                .color(innerColor4b.toARGB())
        }
    }
}

/**
 * Function to draw the outline of a circle of the size [radius]
 *
 * @param radius The radius
 * @param color4b The color
 */
fun RenderEnvironment.drawCircleOutline(radius: Float, color4b: Color4b) {
    drawCustomMesh(
        DrawMode.DEBUG_LINE_STRIP,
        VertexInputType.PosColor,
    ) { matrix ->
        for (p in circlePoints) {
            val point = p * radius

            vertex(matrix, point.x, point.y, point.z)
                .color(color4b.toARGB())
        }
    }
}

fun RenderEnvironment.drawGradientSides(
    height: Double,
    baseColor: Color4b,
    topColor: Color4b,
    box: Box
) {
    if (height == 0.0) {
        return
    }

    val vertexColors =
        listOf(
            baseColor,
            topColor,
            topColor,
            baseColor
        )

    drawGradientQuad(
        listOf(
            Vec3(box.minX, 0.0, box.minZ),
            Vec3(box.minX, height, box.minZ),
            Vec3(box.maxX, height, box.minZ),
            Vec3(box.maxX, 0.0, box.minZ),
        ),
        vertexColors
    )
    drawGradientQuad(
        listOf(
            Vec3(box.maxX, 0.0, box.minZ),
            Vec3(box.maxX, height, box.minZ),
            Vec3(box.maxX, height, box.maxZ),
            Vec3(box.maxX, 0.0, box.maxZ),
        ),
        vertexColors
    )
    drawGradientQuad(
        listOf(
            Vec3(box.maxX, 0.0, box.maxZ),
            Vec3(box.maxX, height, box.maxZ),
            Vec3(box.minX, height, box.maxZ),
            Vec3(box.minX, 0.0, box.maxZ),
        ),
        vertexColors
    )
    drawGradientQuad(
        listOf(
            Vec3(box.minX, 0.0, box.maxZ),
            Vec3(box.minX, height, box.maxZ),
            Vec3(box.minX, height, box.minZ),
            Vec3(box.minX, 0.0, box.minZ),
        ),
        vertexColors
    )
}

fun newDrawContext(): DrawContext = DrawContext(mc, mc.bufferBuilders.entityVertexConsumers)

/**
 * Float version of [DrawContext.fill]
 */
@Suppress("LongParameterList")
fun DrawContext.fill(x1: Float, y1: Float, x2: Float, y2: Float, z: Float, color: Int) {
    val layer = RenderLayer.getGui()
    var x1 = x1
    var y1 = y1
    var x2 = x2
    var y2 = y2
    val matrix4f = this.matrices.peek().getPositionMatrix()
    if (x1 < x2) {
        val i = x1
        x1 = x2
        x2 = i
    }

    if (y1 < y2) {
        val i = y1
        y1 = y2
        y2 = i
    }

    val vertexConsumer: VertexConsumer = (this as MixinDrawContextAccessor).vertexConsumers.getBuffer(layer)
    vertexConsumer.vertex(matrix4f, x1, y1, z).color(color)
    vertexConsumer.vertex(matrix4f, x1, y2, z).color(color)
    vertexConsumer.vertex(matrix4f, x2, y2, z).color(color)
    vertexConsumer.vertex(matrix4f, x2, y1, z).color(color)
}

/**
 * Float version of [DrawContext.drawHorizontalLine]
 */
fun DrawContext.drawHorizontalLine(x1: Float, x2: Float, y: Float, thickness: Float, color: Int) {
    this.fill(x1, y, x2, y + thickness, 0f, color)
}

/**
 * Float version of [DrawContext.drawVerticalLine]
 */
fun DrawContext.drawVerticalLine(x: Float, y1: Float, y2: Float, thickness: Float, color: Int) {
    this.fill(x, y1, x + thickness, y2, 0f, color)
}
