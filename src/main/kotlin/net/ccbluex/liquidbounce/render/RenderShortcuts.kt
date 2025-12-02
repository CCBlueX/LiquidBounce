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

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.VertexFormat
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.UV2f
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.SAMPLER_NAMES
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.*
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import org.lwjgl.opengl.GL11C
import java.util.OptionalDouble
import java.util.OptionalInt
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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
 * Helper function to render an environment with the specified [matrixStack] and [draw] block.
 *
 * @param matrixStack The matrix stack for rendering.
 * @param draw The block of code to be executed in the rendering environment.
 */
@OptIn(ExperimentalContracts::class)
inline fun renderEnvironmentForWorld(
    matrixStack: MatrixStack,
    framebuffer: Framebuffer = mc.framebuffer,
    draw: WorldRenderEnvironment.() -> Unit,
) {
    contract {
        callsInPlace(draw, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }

    val camera = mc.entityRenderDispatcher.camera ?: return

    GL11C.glEnable(GL11C.GL_LINE_SMOOTH)

    val environment = WorldRenderEnvironment(framebuffer, matrixStack, camera)
    draw(environment)
    if (environment.isBatchMode) environment.commitBatch()

    GL11C.glDisable(GL11C.GL_LINE_SMOOTH)
}

inline fun MatrixStack.withPush(block: MatrixStack.() -> Unit) {
    push()
    try {
        block()
    } finally {
        pop()
    }
}

/**
 * Shorthand for `withPosition(relativeToCamera(pos))`
 */
inline fun WorldRenderEnvironment.withPositionRelativeToCamera(pos: Vec3d, draw: WorldRenderEnvironment.() -> Unit) {
    matrixStack.withPush {
        translate(relativeToCamera(pos))
        draw()
    }
}

/**
 * Shortcut of `withPositionRelativeToCamera(Vec3d.of(pos))`
 */
inline fun WorldRenderEnvironment.withPositionRelativeToCamera(pos: Vec3i, draw: WorldRenderEnvironment.() -> Unit) {
    matrixStack.withPush {
        translate(relativeToCamera(pos))
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

inline fun WorldRenderEnvironment.drawCustomMesh(
    pipeline: RenderPipeline,
    drawer: VertexConsumer.(Matrix4f) -> Unit
) {
    val matrix = matrixStack.peek().positionMatrix

    val buffer = getOrCreateBuffer(pipeline)

    drawer(buffer, matrix)

    if (!isBatchMode) {
        buffer.endNullable()?.let {
            pipeline.draw(it)
        }
    }
}

private inline fun WorldRenderEnvironment.drawTexQuads(
    texture: GpuTextureView,
    drawer: VertexConsumer.(Matrix4f) -> Unit
) {
    val matrix = matrixStack.peek().positionMatrix

    val buffer = getOrCreateBuffer(texture)

    drawer(buffer, matrix)

    if (!isBatchMode) {
        buffer.endNullable()?.let {
            RenderSystem.setShaderTexture(0, texture)
            ClientRenderPipelines.TexQuads.draw(it)
        }
    }
}

/**
 * copied from RenderLayer.MultiPhase.draw(BuiltBuffer)
 * @see RenderLayer.MultiPhase.draw
 */
context(env: RenderEnvironment)
@Suppress("detekt:all")
fun RenderPipeline.draw(builtBuffer: BuiltBuffer) = builtBuffer.use { buffer ->
    val gpuBufferSlice = RenderSystem.getDynamicUniforms()
        .write(
            RenderSystem.getModelViewMatrix(),
            Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            RenderSystem.getModelOffset(),
            RenderSystem.getTextureMatrix(),
            RenderSystem.getShaderLineWidth(),
        )
    val gpuBuffer = vertexFormat.uploadImmediateVertexBuffer(buffer.buffer)
    val gpuBuffer2: GpuBuffer
    val indexType: VertexFormat.IndexType
    if (buffer.sortedBuffer == null) {
        val shapeIndexBuffer = RenderSystem.getSequentialBuffer(buffer.drawParameters.mode)
        gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(buffer.drawParameters.indexCount)
        indexType = shapeIndexBuffer.indexType
    } else {
        gpuBuffer2 = vertexFormat.uploadImmediateIndexBuffer(buffer.sortedBuffer)
        indexType = buffer.drawParameters.indexType
    }

//    val colorTexture = RenderSystem.outputColorTextureOverride
//        ?: env.framebuffer.colorAttachmentView
//    val depthTexture = RenderSystem.outputDepthTextureOverride
//        ?: env.framebuffer.depthAttachmentView.takeIf { env.framebuffer.useDepthAttachment }
//
//    gpuDevice.createCommandEncoder().createRenderPass(
//        { "${LiquidBounce.CLIENT_NAME} RenderEnvironment RenderPass" },
//        colorTexture,
//        OptionalInt.empty(),
//        depthTexture,
//        OptionalDouble.empty(),
//    ).use { renderPass ->
    env.framebuffer.createRenderPass().use { renderPass ->
        // TODO: render pass extra actions
        renderPass.setPipeline(this)
        val scissorState = RenderSystem.getScissorStateForRenderTypeDraws()
        if (scissorState.method_72091()) {
            renderPass.enableScissor(
                scissorState.method_72092(),
                scissorState.method_72093(),
                scissorState.method_72094(),
                scissorState.method_72095()
            )
        }

        RenderSystem.bindDefaultUniforms(renderPass)
        renderPass.setUniform("DynamicTransforms", gpuBufferSlice)
        renderPass.setVertexBuffer(0, gpuBuffer)

        for (i in 0..11) {
            val gpuTexture = RenderSystem.getShaderTexture(i)
            if (gpuTexture != null) {
                renderPass.bindSampler(SAMPLER_NAMES[i], gpuTexture)
            }
        }

        renderPass.setIndexBuffer(gpuBuffer2, indexType)
        renderPass.drawIndexed(0, 0, buffer.drawParameters.indexCount, 1)
    }
}

/**
 * Draws a line with endpoint [p1] and [p2] and color [argb].
 */
fun WorldRenderEnvironment.drawLine(p1: Vec3, p2: Vec3, argb: Int) =
    drawCustomMesh(ClientRenderPipelines.Lines) { matrix ->
        vertex(matrix, p1.x, p1.y, p1.z).color(argb)
        vertex(matrix, p2.x, p2.y, p2.z).color(argb)
    }

/**
 * Function to draw lines using the specified [lines] vectors.
 *
 * @param lines The vectors representing the lines.
 */
fun WorldRenderEnvironment.drawLines(argb: Int, vararg lines: Vec3) {
    drawLines(
        lines,
        pipeline = ClientRenderPipelines.Lines,
        argb = argb,
    )
}

/**
 * Function to draw a line strip using the specified [positions] vectors.
 *
 * @param positions The vectors representing the line strip.
 */
fun WorldRenderEnvironment.drawLineStrip(argb: Int, vararg positions: Vec3) {
    drawLines(
        positions,
        pipeline = ClientRenderPipelines.LineStrip,
        argb = argb,
    )
}

/**
 * Helper function to draw lines using the specified [lines] vectors and [pipeline].
 *
 * @param lines The vectors representing the lines.
 * @param pipeline The render pipeline for the lines.
 */
private fun WorldRenderEnvironment.drawLines(
    lines: Array<out Vec3>,
    pipeline: RenderPipeline,
    argb: Int,
) {
    // If the array of lines is empty, we don't need to draw anything
    if (lines.isEmpty()) {
        return
    }

    drawCustomMesh(pipeline) { matrix ->
        lines.forEach { (x, y, z) ->
            vertex(matrix, x, y, z).color(argb)
        }
    }
}

fun WorldRenderEnvironment.drawSquareTexture(
    size: Float,
    argb: Int,
) = drawCustomMesh(ClientRenderPipelines.TexQuads) { matrix ->
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

fun WorldRenderEnvironment.drawTextureQuad(
    textureView: GpuTextureView,
    pos1: Vector3fc,
    uv1: UV2f = UV2f(0f, 0f),
    pos2: Vector3fc,
    uv2: UV2f = UV2f(1f, 1f),
    argb: Int,
) {
    drawTexQuads(textureView) { matrix ->
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

fun WorldRenderEnvironment.drawTriangle(p1: Vec3, p2: Vec3, p3: Vec3, argb: Int) {
    drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->
        vertex(matrix, p1.x, p1.y, p1.z).color(argb)
        vertex(matrix, p2.x, p2.y, p2.z).color(argb)
        vertex(matrix, p3.x, p3.y, p3.z).color(argb)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun VertexConsumer.color(color: Color4b): VertexConsumer = color(color.toARGB())

/**
 * Helper unction to draw a solid box using the specified [box].
 *
 * @param box The bounding box of the box.
 */
@Suppress("CognitiveComplexMethod")
private fun WorldRenderEnvironment.drawBox(
    box: Box,
    pipeline: RenderPipeline,
    useOutlineVertices: Boolean = false,
    color: Color4b,
    verticesToUse: Int = -1
) = drawCustomMesh(pipeline) { matrix ->
    val check = verticesToUse != -1

    // Draw the vertices of the box
    if (useOutlineVertices) {
        box.forEachOutlineVertex { i, x, y, z ->
            if (check && (verticesToUse and (1 shl i)) != 0) {
                return@forEachOutlineVertex
            }

            vertex(matrix, x.toFloat(), y.toFloat(), z.toFloat())
                .color(color.toARGB())
        }
    } else {
        box.forEachFaceVertex { i, x, y, z ->
            if (check && (verticesToUse and (1 shl i)) != 0) {
                return@forEachFaceVertex
            }

            vertex(matrix, x.toFloat(), y.toFloat(), z.toFloat())
                .color(color.toARGB())
        }
    }
}

/**
 * Function to draw a colored [box].
 */
fun WorldRenderEnvironment.drawBox(
    box: Box,
    faceColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
    faceVertices: Int = -1,
    outlineVertices: Int = -1,
) {
    if (faceColor != null && !faceColor.isTransparent) {
        drawBox(box, ClientRenderPipelines.Quads, color = faceColor, verticesToUse = faceVertices)
    }

    if (outlineColor != null && !outlineColor.isTransparent) {
        drawBox(box, ClientRenderPipelines.Lines, useOutlineVertices = true, outlineColor, outlineVertices)
    }
}

/**
 * Function to draw a colored [box] with specified [side].
 */
fun WorldRenderEnvironment.drawBoxSide(
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
 * Function to draw a flat plane on the XZ axis with an optional outline.
 */
fun WorldRenderEnvironment.drawPlane(
    sizeX: Float,
    sizeZ: Float,
    fillColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT
) {
    if (fillColor != null && !fillColor.isTransparent) {
        val argb = fillColor.toARGB()
        drawCustomMesh(ClientRenderPipelines.Quads) { matrix ->
            vertex(matrix, 0f, 0f, 0f).color(argb)
            vertex(matrix, 0f, 0f, sizeZ).color(argb)
            vertex(matrix, sizeX, 0f, sizeZ).color(argb)
            vertex(matrix, sizeX, 0f, 0f).color(argb)
        }
    }

    if (outlineColor != null && !outlineColor.isTransparent) {
        val argb = outlineColor.toARGB()
        drawCustomMesh(ClientRenderPipelines.Lines) { matrix ->
            vertex(matrix, 0f, 0f, 0f).color(argb)
            vertex(matrix, 0f, 0f, sizeZ).color(argb)

            vertex(matrix, 0f, 0f, sizeZ).color(argb)
            vertex(matrix, sizeX, 0f, sizeZ).color(argb)

            vertex(matrix, sizeX, 0f, sizeZ).color(argb)
            vertex(matrix, sizeX, 0f, 0f).color(argb)

            vertex(matrix, sizeX, 0f, 0f).color(argb)
            vertex(matrix, 0f, 0f, 0f).color(argb)
        }
    }
}

/**
 * Function to render a gradient quad using specified [vertices] and [colors]
 *
 * @param vertices The four vectors to draw the quad
 * @param colors The colors for the vertices
 */
private fun WorldRenderEnvironment.drawGradientQuad(vertices: Array<Vec3>, colors: Array<Color4b>) {
    require(vertices.size == colors.size) { "there must be a color for every vertex" }
    require(vertices.size % 4 == 0) { "vertices must be dividable by 4" }
    drawCustomMesh(ClientRenderPipelines.Quads) { matrix ->
        vertices.forEachIndexed { index, (x, y, z) ->
            val color4b = colors[index]
            vertex(matrix, x, y, z).color(color4b.toARGB())
        }
    }
}

private const val CIRCLE_RES = 40

// using a val instead of a function for better performance
private val circlePoints: Array<Vector3fc> = Array(CIRCLE_RES + 1) {
    val theta = MathHelper.PI * 2f * it / CIRCLE_RES
    Vector3f(theta.fastCos(), 0f, theta.fastSin())
}

/**
 * Function to draw a circle of the size [outerRadius] with a cutout of size [innerRadius]
 *
 * @param outerRadius The radius of the circle
 * @param innerRadius The radius inside the circle (the cutout)
 * @param outerColor4b The color of the outer edges
 * @param innerColor4b The color of the inner edges
 */
fun WorldRenderEnvironment.drawGradientCircle(
    outerRadius: Float,
    innerRadius: Float,
    outerColor4b: Color4b,
    innerColor4b: Color4b,
    innerOffset: Vector3fc = Vector3f(),
) {
    drawCustomMesh(ClientRenderPipelines.TriangleStrip) { matrix ->
        val innerP = Vector3f()
        val outerP = Vector3f()
        for (p in circlePoints) {
            outerP.set(p).mul(outerRadius)
            innerP.set(p).mul(innerRadius).add(innerOffset)

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
fun WorldRenderEnvironment.drawCircleOutline(radius: Float, color4b: Color4b) =
    drawCustomMesh(ClientRenderPipelines.LineStrip) { matrix ->
        val point = Vector3f()
        for (p in circlePoints) {
            point.set(p).mul(radius)

            vertex(matrix, point.x, point.y, point.z)
                .color(color4b.toARGB())
        }
    }

fun WorldRenderEnvironment.drawGradientSides(
    height: Double,
    baseColor: Color4b,
    topColor: Color4b,
    box: Box
) {
    if (height == 0.0) {
        return
    }

    val vertexColors =
        arrayOf(
            baseColor,
            topColor,
            topColor,
            baseColor
        )

    drawGradientQuad(
        arrayOf(
            Vec3(box.minX, 0.0, box.minZ),
            Vec3(box.minX, height, box.minZ),
            Vec3(box.maxX, height, box.minZ),
            Vec3(box.maxX, 0.0, box.minZ),
        ),
        vertexColors
    )
    drawGradientQuad(
        arrayOf(
            Vec3(box.maxX, 0.0, box.minZ),
            Vec3(box.maxX, height, box.minZ),
            Vec3(box.maxX, height, box.maxZ),
            Vec3(box.maxX, 0.0, box.maxZ),
        ),
        vertexColors
    )
    drawGradientQuad(
        arrayOf(
            Vec3(box.maxX, 0.0, box.maxZ),
            Vec3(box.maxX, height, box.maxZ),
            Vec3(box.minX, height, box.maxZ),
            Vec3(box.minX, 0.0, box.maxZ),
        ),
        vertexColors
    )
    drawGradientQuad(
        arrayOf(
            Vec3(box.minX, 0.0, box.maxZ),
            Vec3(box.minX, height, box.maxZ),
            Vec3(box.minX, height, box.minZ),
            Vec3(box.minX, 0.0, box.minZ),
        ),
        vertexColors
    )
}
