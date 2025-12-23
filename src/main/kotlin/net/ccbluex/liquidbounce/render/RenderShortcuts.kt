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
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.mc
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.world.phys.AABB
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraft.core.Vec3i
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import org.lwjgl.opengl.GL11C
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.function.Supplier
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
val FULL_BOX = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

@JvmField
val EMPTY_BOX = AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

/**
 * Helper function to render an environment with the specified [matrixStack] and [draw] block.
 *
 * @param matrixStack The matrix stack for rendering.
 * @param draw The block of code to be executed in the rendering environment.
 */
@OptIn(ExperimentalContracts::class)
inline fun renderEnvironmentForWorld(
    matrixStack: PoseStack,
    framebuffer: RenderTarget = mc.mainRenderTarget,
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

inline fun PoseStack.withPush(block: PoseStack.() -> Unit) {
    pushPose()
    try {
        block()
    } finally {
        popPose()
    }
}

/**
 * Shorthand for `withPosition(relativeToCamera(pos))`
 */
inline fun WorldRenderEnvironment.withPositionRelativeToCamera(pos: Vec3, draw: WorldRenderEnvironment.() -> Unit) {
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
    val matrix = matrixStack.last().pose()

    val buffer = getOrCreateBuffer(pipeline)

    drawer(buffer, matrix)

    if (!isBatchMode) {
        buffer.build()?.let {
            draw(pipeline, it)
        }
    }
}

/**
 * copied from RenderLayer.draw(BuiltBuffer) (1.21.5-10: RenderLayer.MultiPhase.draw)
 * @see net.minecraft.client.renderer.rendertype.RenderType.draw
 */
@Suppress("detekt:all")
fun drawMesh(
    pipeline: RenderPipeline,
    builtBuffer: MeshData,
    framebuffer: RenderTarget = mc.mainRenderTarget,
    shaderColor: Vector4f = Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
    renderPassLabelGetter: Supplier<String> = Supplier { "${LiquidBounce.CLIENT_NAME} RenderEnvironment RenderPass" },
    shaderTextureProvider: Map<String, AbstractTexture> = emptyMap(),
) = builtBuffer.use { buffer ->
    val gpuBufferSlice = RenderSystem.getDynamicUniforms()
        .writeTransform(
            RenderSystem.getModelViewMatrix(),
            shaderColor,
            Vector3f(),
            Matrix4f(),
        )
    val gpuBuffer = pipeline.vertexFormat.uploadImmediateVertexBuffer(buffer.vertexBuffer())
    val gpuBuffer2: GpuBuffer
    val indexType: VertexFormat.IndexType
    if (buffer.indexBuffer() == null) {
        val shapeIndexBuffer = RenderSystem.getSequentialBuffer(buffer.drawState().mode)
        gpuBuffer2 = shapeIndexBuffer.getBuffer(buffer.drawState().indexCount)
        indexType = shapeIndexBuffer.type()
    } else {
        gpuBuffer2 = pipeline.vertexFormat.uploadImmediateIndexBuffer(buffer.indexBuffer()!!)
        indexType = buffer.drawState().indexType
    }

    val colorTexture = RenderSystem.outputColorTextureOverride
        ?: framebuffer.colorTextureView!!
    val depthTexture = RenderSystem.outputDepthTextureOverride
        ?: framebuffer.depthTextureView.takeIf { framebuffer.useDepth }

    gpuDevice.createCommandEncoder().createRenderPass(
        renderPassLabelGetter,
        colorTexture,
        OptionalInt.empty(),
        depthTexture,
        OptionalDouble.empty(),
    ).use { renderPass ->
        renderPass.setPipeline(pipeline)
        val scissorState = RenderSystem.getScissorStateForRenderTypeDraws()
        if (scissorState.enabled()) {
            renderPass.enableScissor(
                scissorState.x(),
                scissorState.y(),
                scissorState.width(),
                scissorState.height()
            )
        }

        RenderSystem.bindDefaultUniforms(renderPass)
        renderPass.setUniform("DynamicTransforms", gpuBufferSlice)
        renderPass.setVertexBuffer(0, gpuBuffer)

        for ((key, texture) in shaderTextureProvider) {
            renderPass.bindTexture(key, texture.textureView, texture.sampler)
        }

        renderPass.setIndexBuffer(gpuBuffer2, indexType)
        renderPass.drawIndexed(0, 0, buffer.drawState().indexCount, 1)
    }
}

/**
 * Draws a line with endpoint [p1] and [p2] and color [argb].
 */
fun WorldRenderEnvironment.drawLine(p1: Vec3f, p2: Vec3f, argb: Int) =
    drawCustomMesh(ClientRenderPipelines.Lines) { matrix ->
        addVertex(matrix, p1.x, p1.y, p1.z).setColor(argb)
        addVertex(matrix, p2.x, p2.y, p2.z).setColor(argb)
    }

/**
 * Function to draw lines using the specified [lines] vectors.
 *
 * @param lines The vectors representing the lines.
 */
fun WorldRenderEnvironment.drawLines(argb: Int, vararg lines: Vec3f) {
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
fun WorldRenderEnvironment.drawLineStrip(argb: Int, vararg positions: Vec3f) {
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
    lines: Array<out Vec3f>,
    pipeline: RenderPipeline,
    argb: Int,
) {
    // If the array of lines is empty, we don't need to draw anything
    if (lines.isEmpty()) {
        return
    }

    drawCustomMesh(pipeline) { matrix ->
        lines.forEach { (x, y, z) ->
            addVertex(matrix, x, y, z).setColor(argb)
        }
    }
}

fun WorldRenderEnvironment.drawSquareTexture(
    size: Float,
    argb: Int,
) = drawCustomMesh(ClientRenderPipelines.TexQuads) { matrix ->
    addVertex(matrix, 0.0f, -size, 0.0f)
        .setUv(0.0f, 0.0f)
        .setColor(argb)

    addVertex(matrix, -size, -size, 0.0f)
        .setUv(0.0f, 1.0f)
        .setColor(argb)

    addVertex(matrix, -size, 0.0f, 0.0f)
        .setUv(1.0f, 1.0f)
        .setColor(argb)

    addVertex(matrix, 0.0f, 0.0f, 0.0f)
        .setUv(1.0f, 0.0f)
        .setColor(argb)
}

fun WorldRenderEnvironment.drawTriangle(p1: Vec3f, p2: Vec3f, p3: Vec3f, argb: Int) {
    drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->
        addVertex(matrix, p1.x, p1.y, p1.z).setColor(argb)
        addVertex(matrix, p2.x, p2.y, p2.z).setColor(argb)
        addVertex(matrix, p3.x, p3.y, p3.z).setColor(argb)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun VertexConsumer.color(color: Color4b): VertexConsumer = setColor(color.toARGB())

/**
 * Helper unction to draw a solid box using the specified [box].
 *
 * @param box The bounding box of the box.
 */
@Suppress("CognitiveComplexMethod")
private fun WorldRenderEnvironment.drawBox(
    box: AABB,
    pipeline: RenderPipeline,
    useOutlineVertices: Boolean = false,
    color: Color4b,
    verticesToUse: Int = -1,
) = drawCustomMesh(pipeline) { matrix ->
    val check = verticesToUse and 0xFFFFFF != 0xFFFFFF

    // Draw the vertices of the box
    if (useOutlineVertices) {
        box.forEachOutlineVertex { i, x, y, z ->
            if (check && (verticesToUse and (1 shl i)) == 0) {
                return@forEachOutlineVertex
            }

            addVertex(matrix, x.toFloat(), y.toFloat(), z.toFloat())
                .setColor(color.toARGB())
        }
    } else {
        box.forEachFaceVertex { i, x, y, z ->
            if (check && (verticesToUse and (1 shl i)) == 0) {
                return@forEachFaceVertex
            }

            addVertex(matrix, x.toFloat(), y.toFloat(), z.toFloat())
                .setColor(color.toARGB())
        }
    }
}

/**
 * Function to draw a colored [box].
 */
fun WorldRenderEnvironment.drawBox(
    box: AABB,
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
    box: AABB,
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
 * Function to draw a colored [box] with specified [sides].
 */
fun WorldRenderEnvironment.drawBoxSides(
    box: AABB,
    sides: Iterable<Direction>,
    faceColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
) = drawBox(
    box,
    faceColor,
    outlineColor,
    faceVertices = sides.fold(0) { acc, side -> acc or BoxVertexIterator.FACE.sideMask(side) },
    outlineVertices = sides.fold(0) { acc, side -> acc or BoxVertexIterator.OUTLINE.sideMask(side) },
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
            addVertex(matrix, 0f, 0f, 0f).setColor(argb)
            addVertex(matrix, 0f, 0f, sizeZ).setColor(argb)
            addVertex(matrix, sizeX, 0f, sizeZ).setColor(argb)
            addVertex(matrix, sizeX, 0f, 0f).setColor(argb)
        }
    }

    if (outlineColor != null && !outlineColor.isTransparent) {
        val argb = outlineColor.toARGB()
        drawCustomMesh(ClientRenderPipelines.Lines) { matrix ->
            addVertex(matrix, 0f, 0f, 0f).setColor(argb)
            addVertex(matrix, 0f, 0f, sizeZ).setColor(argb)

            addVertex(matrix, 0f, 0f, sizeZ).setColor(argb)
            addVertex(matrix, sizeX, 0f, sizeZ).setColor(argb)

            addVertex(matrix, sizeX, 0f, sizeZ).setColor(argb)
            addVertex(matrix, sizeX, 0f, 0f).setColor(argb)

            addVertex(matrix, sizeX, 0f, 0f).setColor(argb)
            addVertex(matrix, 0f, 0f, 0f).setColor(argb)
        }
    }
}

/**
 * Function to render a gradient quad using specified [vertices] and [colors]
 *
 * @param vertices The four vectors to draw the quad
 * @param colors The colors for the vertices
 */
private fun WorldRenderEnvironment.drawGradientQuad(vertices: Array<Vec3f>, colors: Array<Color4b>) {
    require(vertices.size == colors.size) { "there must be a color for every vertex" }
    require(vertices.size % 4 == 0) { "vertices must be dividable by 4" }
    drawCustomMesh(ClientRenderPipelines.Quads) { matrix ->
        vertices.forEachIndexed { index, (x, y, z) ->
            val color4b = colors[index]
            addVertex(matrix, x, y, z).setColor(color4b.toARGB())
        }
    }
}

private const val CIRCLE_RES = 40

// using a val instead of a function for better performance
private val circlePoints: Array<Vector3fc> = Array(CIRCLE_RES + 1) {
    val theta = Mth.PI * 2f * it / CIRCLE_RES
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

            addVertex(matrix, outerP.x, outerP.y, outerP.z)
                .setColor(outerColor4b.toARGB())
            addVertex(matrix, innerP.x, innerP.y, innerP.z)
                .setColor(innerColor4b.toARGB())
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

            addVertex(matrix, point.x, point.y, point.z)
                .setColor(color4b.toARGB())
        }
    }

fun WorldRenderEnvironment.drawGradientSides(
    height: Double,
    baseColor: Color4b,
    topColor: Color4b,
    box: AABB
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
            Vec3f(box.minX, 0.0, box.minZ),
            Vec3f(box.minX, height, box.minZ),
            Vec3f(box.maxX, height, box.minZ),
            Vec3f(box.maxX, 0.0, box.minZ),
        ),
        vertexColors
    )
    drawGradientQuad(
        arrayOf(
            Vec3f(box.maxX, 0.0, box.minZ),
            Vec3f(box.maxX, height, box.minZ),
            Vec3f(box.maxX, height, box.maxZ),
            Vec3f(box.maxX, 0.0, box.maxZ),
        ),
        vertexColors
    )
    drawGradientQuad(
        arrayOf(
            Vec3f(box.maxX, 0.0, box.maxZ),
            Vec3f(box.maxX, height, box.maxZ),
            Vec3f(box.minX, height, box.maxZ),
            Vec3f(box.minX, 0.0, box.maxZ),
        ),
        vertexColors
    )
    drawGradientQuad(
        arrayOf(
            Vec3f(box.minX, 0.0, box.maxZ),
            Vec3f(box.minX, height, box.maxZ),
            Vec3f(box.minX, height, box.minZ),
            Vec3f(box.minX, 0.0, box.minZ),
        ),
        vertexColors
    )
}
