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

@file:Suppress("detekt:TooManyFunctions")

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
import net.ccbluex.fastutil.enumMapOf
import net.ccbluex.fastutil.objectObjectMapOf
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.render.utils.DistanceFadeUniformValueGroup
import net.ccbluex.liquidbounce.render.utils.UnitCircle
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.writeStd140
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4fc
import org.joml.Vector3f
import org.joml.Vector3fc
import org.lwjgl.opengl.GL11C
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
val HAS_AMD_VEGA_APU = (GL11C.glGetString(GL11C.GL_RENDERER)?.startsWith("AMD Radeon(TM) RX Vega") ?: false) &&
    GL11C.glGetString(GL11C.GL_VENDOR) == "ATI Technologies Inc."

@JvmField
val FULL_BOX = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

@JvmField
val EMPTY_BOX = AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

private val ROUNDED_RECT_AS_OUTLINE_CIRCLE_UBO by lazy(LazyThreadSafetyMode.NONE) {
    val slice = ClientUniformDefine.ROUNDED_RECT.createSingleBuffer()
    slice.writeStd140 {
        putVec2(1f, 1f)
        putFloat(2f)
    }
    slice
}

private val ROUNDED_RECT_AS_FILLED_CIRCLE_UBO by lazy(LazyThreadSafetyMode.NONE) {
    val slice = ClientUniformDefine.ROUNDED_RECT.createSingleBuffer()
    slice.writeStd140 {
        putVec2(1f, 1f)
        putFloat(0f)
    }
    slice
}

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

inline fun WorldRenderEnvironment.withPositionRelativeToCamera(draw: WorldRenderEnvironment.() -> Unit) {
    matrixStack.withPush {
        translate(camera.position().reverse())
        draw()
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
inline fun WorldRenderEnvironment.longLines(draw: WorldRenderEnvironment.() -> Unit) {
    if (HAS_AMD_VEGA_APU) GL11C.glDisable(GL11C.GL_LINE_SMOOTH)
    try {
        draw()
    } finally {
        if (HAS_AMD_VEGA_APU) GL11C.glEnable(GL11C.GL_LINE_SMOOTH)
    }
}

internal inline fun RenderTarget.drawGenericBlockESP(
    renderState: RenderPassRenderState,
    pipeline: RenderPipeline,
    distanceFade: DistanceFadeUniformValueGroup,
    dynamicTransforms: () -> GpuBufferSlice = ::getDynamicTransformsUniform,
): Boolean {
    if (!renderState.ready) return false

    distanceFade.updateIfDirty()
    val dynamicTransforms = dynamicTransforms()
    this.createRenderPass({ renderState.label + " Pass" }).use { pass ->
        pass.setPipeline(pipeline)

        pass.bindProjectionUniform()
        pass.bindGlobalsUniform()
        pass.bindDynamicTransformsUniform(dynamicTransforms)
        distanceFade.bindUniform(pass)
        renderState.bindAndDraw(pass)
    }
    return true
}

inline fun WorldRenderEnvironment.drawCustomMeshTextured(
    sampler0: AbstractTexture,
    pipeline: RenderPipeline = ClientRenderPipelines.TexQuads, // TODO: implement this
    drawer: VertexConsumer.(Matrix4fc) -> Unit,
) {
    val matrix = matrixStack.last().pose()

    val buffer = getOrCreateBuffer(sampler0)

    drawer(buffer, matrix)

    if (!isBatchMode) {
        buffer.build()?.let {
            draw(pipeline, it, shaderTextureProvider = objectObjectMapOf("Sampler0", sampler0))
        }
    }
}

inline fun WorldRenderEnvironment.drawCustomMesh(
    pipeline: RenderPipeline,
    drawer: VertexConsumer.(PoseStack.Pose) -> Unit,
) {
    val matrix = matrixStack.last()

    val buffer = getOrCreateBuffer(pipeline)

    drawer(buffer, matrix)

    if (!isBatchMode) {
        buffer.build()?.let {
            draw(pipeline, it, emptyMap())
        }
    }
}

private val sharedVboMap = Object2ObjectArrayMap<VertexFormat, GrowableMappableRingBuffer>()
private fun getVbo(vertexFormat: VertexFormat): GrowableMappableRingBuffer =
    sharedVboMap.computeIfAbsent(vertexFormat) {
        GrowableMappableRingBuffer(
            "${LiquidBounce.CLIENT_NAME} Shared VBO for $it",
            GpuBuffer.USAGE_VERTEX,
            GrowableMappableRingBuffer.GrowPolicy.of(paddingScale = 8, min = 1 shl 13)
        )
    }

private val sharedIboMap = enumMapOf<VertexFormat.IndexType, GrowableMappableRingBuffer>()
private fun getIbo(indexType: VertexFormat.IndexType): GrowableMappableRingBuffer =
    sharedIboMap.computeIfAbsent(indexType) {
        GrowableMappableRingBuffer(
            "${LiquidBounce.CLIENT_NAME} Shared IBO for $it",
            GpuBuffer.USAGE_INDEX,
        )
    }

/**
 * copied from RenderLayer.draw(BuiltBuffer) (1.21.5-10: RenderLayer.MultiPhase.draw)
 * @see net.minecraft.client.renderer.rendertype.RenderType.draw
 */
@Suppress("detekt:all")
internal fun drawMesh(
    pipeline: RenderPipeline,
    meshData: MeshData,
    renderTarget: RenderTarget = mc.mainRenderTarget,
    colorModulator: Color4b = Color4b.WHITE,
    renderPassLabelGetter: Supplier<String> = Supplier { "${LiquidBounce.CLIENT_NAME} RenderEnvironment RenderPass" },
    shaderTextures: Map<String, AbstractTexture> = emptyMap(),
    uniforms: Map<String, GpuBufferSlice> = emptyMap(),
) = meshData.use { meshData ->
    val dynamicTransforms = getDynamicTransformsUniform(colorModulator = colorModulator)

    if (pipeline.vertexFormatMode == VertexFormat.Mode.QUADS) {
        meshData.sortQuads(
            ClientTesselator.Shared,
            RenderSystem.getProjectionType().vertexSorting(),
        )
    }

    val vertexSlice = getVbo(pipeline.vertexFormat).upload(meshData.vertexBuffer())

    val rawIndices = meshData.indexBuffer()
    val indexCount = meshData.drawState().indexCount
    val indexSlice: GpuBufferSlice
    val indexType: VertexFormat.IndexType
    if (rawIndices == null) {
        val shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.vertexFormatMode)
        indexType = shapeIndexBuffer.type()
        indexSlice = shapeIndexBuffer.getBuffer(indexCount)
            .slice(0L, indexCount.toLong() * indexType.bytes)
    } else {
        indexType = meshData.drawState().indexType
        indexSlice = getIbo(indexType).upload(rawIndices)
    }

    renderTarget.createRenderPass(renderPassLabelGetter, allowOverride = true).use { renderPass ->
        renderPass.setPipeline(pipeline)
        renderPass.setupRenderTypeScissor()
        renderPass.bindDefaultUniforms()
        renderPass.bindDynamicTransformsUniform(dynamicTransforms)
        renderPass.bindTextures(shaderTextures)
        renderPass.setUniforms(uniforms)

        renderPass.bindAndDraw(vertexSlice, indexSlice, pipeline.vertexFormat, indexType, indexCount)
    }
}

/**
 * Draws a line with endpoint [p1] and [p2] and color [argb].
 */
fun WorldRenderEnvironment.drawLine(p1: Vec3f, p2: Vec3f, argb: Int) =
    drawCustomMesh(ClientRenderPipelines.Lines) { pose ->
        addVertex(pose, p1).setColor(argb)
        addVertex(pose, p2).setColor(argb)
    }

/**
 * Draws lines with [width].
 * Modern GL doesn't support `glLineWidth` well, so draw with shader simulation.
 */
fun WorldRenderEnvironment.drawLinesWithWidth(argb: Int, width: Float, vararg positions: Vec3f) {
    if (positions.isEmpty()) return
    require(positions.size and 1 == 0)

    drawCustomMesh(pipeline = ClientRenderPipelines.LinesWithWidth) { pose ->
        for (i in 0 until positions.size step 2) {
            val p1 = positions[i]
            val p2 = positions[i + 1]
            val norm1 = (p1 - p2).normalized()
            addVertex(pose, p1)
                .setColor(argb)
                .setNormal(pose, norm1)
                .setLineWidth(width)
            addVertex(pose, p2)
                .setColor(argb)
                .setNormal(pose, -norm1)
                .setLineWidth(width)
        }
    }
}

/**
 * Function to draw lines using the specified [positions] vectors.
 *
 * @param positions The vectors representing the lines.
 */
fun WorldRenderEnvironment.drawLines(argb: Int, vararg positions: Vec3f) {
    if (positions.isEmpty()) return
    require(positions.size and 1 == 0)

    drawCustomMesh(pipeline = ClientRenderPipelines.Lines) { pose ->
        for (pos in positions) {
            addVertex(pose, pos).setColor(argb)
        }
    }
}

/**
 * Function to draw a line strip using the specified [positions] vectors.
 *
 * @param positions The vectors representing the line strip.
 */
fun WorldRenderEnvironment.drawLineStrip(argb: Int, vararg positions: Vec3f) {
    if (positions.isEmpty()) return

    drawCustomMesh(pipeline = ClientRenderPipelines.LineStrip) { pose ->
        for (pos in positions) {
            addVertex(pose, pos).setColor(argb)
        }
    }
}

/**
 * Function to draw a 'line strip' using the specified [positions] vectors,
 * actual pipeline is [ClientRenderPipelines.Lines].
 *
 * @param positions The vectors representing the line strip, the size should be even.
 */
fun WorldRenderEnvironment.drawLineStripAsLines(argb: Int, positions: Collection<Vec3>) {
    if (positions.isEmpty()) return
    require(positions.size and 1 == 0)

    drawCustomMesh(ClientRenderPipelines.Lines) { pose ->
        positions.forEachIndexed { index, pos ->
            if (index != 0 && index != positions.size - 1) {
                addVertex(pose, pos).setColor(argb)
            }
            addVertex(pose, pos).setColor(argb)
        }
    }
}

fun WorldRenderEnvironment.drawTexQuad(
    sampler0: AbstractTexture,
    argb: Int,
) {
    drawCustomMeshTextured(sampler0) { pose ->
        addVertex(pose, -0.5f, -0.5f, 0f).setUv(0f, 0f).setColor(argb)
        addVertex(pose, -0.5f, 0.5f, 0f).setUv(0f, 1f).setColor(argb)
        addVertex(pose, 0.5f, 0.5f, 0f).setUv(1f, 1f).setColor(argb)
        addVertex(pose, 0.5f, -0.5f, 0f).setUv(1f, 0f).setColor(argb)
    }
}

fun WorldRenderEnvironment.drawSquareTexture(
    sampler0: AbstractTexture,
    size: Float,
    argb: Int,
) = drawCustomMeshTextured(sampler0) { matrix ->
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
        addVertex(matrix, p1).setColor(argb)
        addVertex(matrix, p2).setColor(argb)
        addVertex(matrix, p3).setColor(argb)
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
        drawCustomMesh(ClientRenderPipelines.Quads) { pose ->
            addBoxFaces(pose.pose(), box, color = faceColor, verticesToUse = faceVertices)
        }
    }

    if (outlineColor != null && !outlineColor.isTransparent) {
        drawCustomMesh(ClientRenderPipelines.Lines) { pose ->
            addBoxOutlines(pose.pose(), box, outlineColor, outlineVertices)
        }
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
    faceVertices = BoxVertexIterator.FACE.sideMask(sides),
    outlineVertices = BoxVertexIterator.OUTLINE.sideMask(sides),
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
        val argb = fillColor.argb
        drawCustomMesh(ClientRenderPipelines.Quads) { matrix ->
            addVertex(matrix, 0f, 0f, 0f).setColor(argb)
            addVertex(matrix, 0f, 0f, sizeZ).setColor(argb)
            addVertex(matrix, sizeX, 0f, sizeZ).setColor(argb)
            addVertex(matrix, sizeX, 0f, 0f).setColor(argb)
        }
    }

    if (outlineColor != null && !outlineColor.isTransparent) {
        val argb = outlineColor.argb
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
 * Function to draw a circle of the size [outerRadius] with a cutout of size [innerRadius]
 *
 * @param outerRadius The radius of the circle
 * @param innerRadius The radius inside the circle (the cutout)
 * @param outerColor The color of the outer edges
 * @param innerColor The color of the inner edges
 */
fun WorldRenderEnvironment.drawGradientCircle(
    outerRadius: Float,
    innerRadius: Float,
    outerColor: Color4b,
    innerColor: Color4b,
    innerOffset: Vector3fc = Vector3f(),
    noDepthTest: Boolean = true,
) {
    drawCustomMesh(ClientRenderPipelines.triangleStrip(noDepthTest)) { matrix ->
        val innerP = Vector3f()
        val outerP = Vector3f()
        UnitCircle.forEach { cosine, sine ->
            outerP.set(cosine * outerRadius, 0f, sine * outerRadius)
            innerP.set(cosine * innerRadius, 0f, sine * innerRadius).add(innerOffset)

            addVertex(matrix, outerP).setColor(outerColor.argb)
            addVertex(matrix, innerP).setColor(innerColor.argb)
        }
    }
}

private fun WorldRenderEnvironment.drawCircleXZNoUniform(radius: Float, argb: Int, noDepthTest: Boolean) {
    drawCustomMesh(ClientRenderPipelines.roundedRect(noDepthTest)) { pose ->
        addVertex(pose, -radius, 0f, -radius).setUv(0f, 0f).setColor(argb)
        addVertex(pose, -radius, 0f, radius).setUv(0f, 1f).setColor(argb)
        addVertex(pose, radius, 0f, radius).setUv(1f, 1f).setColor(argb)
        addVertex(pose, radius, 0f, -radius).setUv(1f, 0f).setColor(argb)
    }
}

fun WorldRenderEnvironment.drawCircle(
    radius: Float,
    color: Color4b,
) {
    uniform(ClientUniformDefine.ROUNDED_RECT.uboName, ROUNDED_RECT_AS_FILLED_CIRCLE_UBO)
    drawCircleXZNoUniform(radius, color.argb, noDepthTest = true)
}

fun WorldRenderEnvironment.drawCircle(
    radius: Float,
    innerColor: Color4b,
    outerColor: Color4b,
) {
    if (innerColor == outerColor) {
        drawCircle(radius, color = innerColor)
        return
    }

    uniform(ClientUniformDefine.ROUNDED_RECT.uboName, ROUNDED_RECT_AS_FILLED_CIRCLE_UBO)
    drawCustomMesh(ClientRenderPipelines.roundedRect(noDepthTest = true)) { pose ->
        // Quad 1 (NW)
        addVertex(pose, -radius, 0f, -radius).setUv(0f, 0f).setColor(outerColor)
        addVertex(pose, -radius, 0f, 0f).setUv(0f, 0.5f).setColor(outerColor)
        addVertex(pose, 0f, 0f, 0f).setUv(0.5f, 0.5f).setColor(innerColor)
        addVertex(pose, 0f, 0f, -radius).setUv(0.5f, 0f).setColor(outerColor)

        // Quad 2 (NE)
        addVertex(pose, radius, 0f, -radius).setUv(1f, 0f).setColor(outerColor)
        addVertex(pose, 0f, 0f, -radius).setUv(0.5f, 0f).setColor(outerColor)
        addVertex(pose, 0f, 0f, 0f).setUv(0.5f, 0.5f).setColor(innerColor)
        addVertex(pose, radius, 0f, 0f).setUv(1f, 0.5f).setColor(outerColor)

        // Quad 3 (SE)
        addVertex(pose, radius, 0f, radius).setUv(1f, 1f).setColor(outerColor)
        addVertex(pose, radius, 0f, 0f).setUv(1f, 0.5f).setColor(outerColor)
        addVertex(pose, 0f, 0f, 0f).setUv(0.5f, 0.5f).setColor(innerColor)
        addVertex(pose, 0f, 0f, radius).setUv(0.5f, 1f).setColor(outerColor)

        // Quad 4 (SW)
        addVertex(pose, -radius, 0f, radius).setUv(0f, 1f).setColor(outerColor)
        addVertex(pose, 0f, 0f, radius).setUv(0.5f, 1f).setColor(outerColor)
        addVertex(pose, 0f, 0f, 0f).setUv(0.5f, 0.5f).setColor(innerColor)
        addVertex(pose, -radius, 0f, 0f).setUv(0f, 0.5f).setColor(outerColor)
    }
}

/**
 * Function to draw the outline of a circle of the size [radius]
 *
 * @param radius The radius
 * @param color The color
 */
@JvmOverloads
fun WorldRenderEnvironment.drawCircleOutline(radius: Float, color: Color4b, noDepthTest: Boolean = true) {
    uniform(ClientUniformDefine.ROUNDED_RECT.uboName, ROUNDED_RECT_AS_OUTLINE_CIRCLE_UBO)
    drawCircleXZNoUniform(radius, color.argb, noDepthTest)
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

    drawCustomMesh(ClientRenderPipelines.Quads) { pose ->
        addVertex(pose, box.minX, 0.0, box.minZ).setColor(baseColor)
        addVertex(pose, box.minX, height, box.minZ).setColor(topColor)
        addVertex(pose, box.maxX, height, box.minZ).setColor(topColor)
        addVertex(pose, box.maxX, 0.0, box.minZ).setColor(baseColor)

        addVertex(pose, box.maxX, 0.0, box.minZ).setColor(baseColor)
        addVertex(pose, box.maxX, height, box.minZ).setColor(topColor)
        addVertex(pose, box.maxX, height, box.maxZ).setColor(topColor)
        addVertex(pose, box.maxX, 0.0, box.maxZ).setColor(baseColor)

        addVertex(pose, box.maxX, 0.0, box.maxZ).setColor(baseColor)
        addVertex(pose, box.maxX, height, box.maxZ).setColor(topColor)
        addVertex(pose, box.minX, height, box.maxZ).setColor(topColor)
        addVertex(pose, box.minX, 0.0, box.maxZ).setColor(baseColor)

        addVertex(pose, box.minX, 0.0, box.maxZ).setColor(baseColor)
        addVertex(pose, box.minX, height, box.maxZ).setColor(topColor)
        addVertex(pose, box.minX, height, box.minZ).setColor(topColor)
        addVertex(pose, box.minX, 0.0, box.minZ).setColor(baseColor)
    }
}
