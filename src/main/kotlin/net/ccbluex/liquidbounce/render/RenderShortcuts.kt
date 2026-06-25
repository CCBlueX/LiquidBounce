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

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.ccbluex.fastutil.objectObjectMapOf
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.render.utils.DistanceFadeUniformValueGroup
import net.ccbluex.liquidbounce.render.utils.UnitCircle
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.writeStd140
import net.minecraft.client.Camera
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector3f
import org.joml.Vector3fc

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
val HAS_AMD_VEGA_APU = gpuDevice.deviceInfo.name.startsWith("AMD Radeon(TM) RX Vega") &&
    gpuDevice.deviceInfo.vendorName == "ATI Technologies Inc."

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

/**
 * Helper function to render an environment with the specified [poseStack] and [draw] block.
 *
 * @param poseStack The matrix stack for rendering.
 * @param mode The default draw mode for [draw].
 * @param draw The block of code to be executed in the rendering environment.
 */
inline fun renderEnvironmentForWorld(
    poseStack: PoseStack,
    renderTarget: RenderTarget = mc.gameRenderer.mainRenderTarget(),
    mode: DrawMode = DrawMode.BATCH,
    camera: Camera = mc.gameRenderer.mainCamera(),
    draw: WorldRenderEnvironment.() -> Unit,
) {
    val environment = WorldRenderEnvironment.create(renderTarget, poseStack, camera)
    try {
        when (mode) {
            DrawMode.BATCH -> environment.batch(draw)
            DrawMode.IMMEDIATE -> environment.immediate(draw)
        }
    } finally {
        environment.flushBatchIfLocalEnvironment()
    }
}

inline fun WorldRenderEnvironment.withPositionRelativeToCamera(draw: WorldRenderEnvironment.() -> Unit) {
    poseStack.withPush {
        translate(camera.position().reverse())
        draw()
    }
}

inline fun WorldRenderEnvironment.withPositionRelativeToCamera(
    x: Double, y: Double, z: Double, draw: WorldRenderEnvironment.() -> Unit
) {
    poseStack.withPush {
        val cameraPos = camera.position()
        translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z)
        draw()
    }
}

/**
 * Shorthand for `withPosition(relativeToCamera(pos))`
 */
inline fun WorldRenderEnvironment.withPositionRelativeToCamera(pos: Vec3, draw: WorldRenderEnvironment.() -> Unit) =
    withPositionRelativeToCamera(pos.x, pos.y, pos.z, draw)

/**
 * Shortcut of `withPositionRelativeToCamera(Vec3.atLowerCornerOf(pos))`
 */
inline fun WorldRenderEnvironment.withPositionRelativeToCamera(pos: Vec3i, draw: WorldRenderEnvironment.() -> Unit) =
    withPositionRelativeToCamera(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), draw)

internal inline fun RenderTarget.drawGenericBlockESP(
    renderState: CachedMeshStorage,
    pipeline: RenderPipeline,
    distanceFade: DistanceFadeUniformValueGroup,
    dynamicTransforms: () -> GpuBufferSlice = ::getDynamicTransformsUniform,
): Boolean {
    if (!renderState.isReady) return false

    distanceFade.updateIfDirty()
    val dynamicTransforms = dynamicTransforms()
    this.createRenderPass({ renderState.label + " Pass" }).use { pass ->
        pass.setPipeline(pipeline)

        pass.bindProjectionUniform()
        pass.bindGlobalsUniform()
        pass.bindDynamicTransformsUniform(dynamicTransforms)
        renderState.bindUniform(pass)
        distanceFade.bindUniform(pass)
        renderState.bindAndDraw(pass)
    }
    return true
}

/**
 * Variant of [drawCustomMesh] that binds [sampler0] as `Sampler0`.
 */
inline fun WorldRenderEnvironment.drawCustomMeshTextured(
    sampler0: AbstractTexture,
    pipeline: RenderPipeline = ClientRenderPipelines.TexQuads,
    uniforms: Map<String, GpuBufferSlice> = emptyMap(),
    drawer: VertexConsumer.(PoseStack.Pose) -> Unit,
) = drawCustomMesh(
    pipeline = pipeline,
    textures = objectObjectMapOf("Sampler0", sampler0),
    uniforms = uniforms,
    drawer = drawer,
)

/**
 * Preferred mesh draw helper for world rendering code.
 */
inline fun WorldRenderEnvironment.drawCustomMesh(
    pipeline: RenderPipeline,
    textures: Map<String, AbstractTexture> = emptyMap(),
    uniforms: Map<String, GpuBufferSlice> = emptyMap(),
    drawer: VertexConsumer.(PoseStack.Pose) -> Unit,
) {
    val buffer = start(
        pipeline = pipeline,
        textures = textures,
        uniforms = uniforms,
    )

    try {
        drawer(buffer, poseStack.last())
    } finally {
        finish(buffer)
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

fun WorldRenderEnvironment.drawShape(
    shape: VoxelShape,
    faceColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
) {
    if (faceColor != null && !faceColor.isTransparent) {
        drawCustomMesh(ClientRenderPipelines.Quads) { pose ->
            addShapeFaces(pose.pose(), shape, color = faceColor)
        }
    }

    if (outlineColor != null && !outlineColor.isTransparent) {
        drawCustomMesh(ClientRenderPipelines.Lines) { pose ->
            addShapeOutlines(pose.pose(), shape, outlineColor)
        }
    }
}

fun WorldRenderEnvironment.drawShapeSide(
    shape: VoxelShape,
    side: Direction,
    hitPos: Vec3,
    faceColor: Color4b? = Color4b.TRANSPARENT,
    outlineColor: Color4b? = Color4b.TRANSPARENT,
) {
    if (faceColor != null && !faceColor.isTransparent) {
        drawCustomMesh(ClientRenderPipelines.Quads) { pose ->
            addShapeSideFaces(pose.pose(), shape, side, hitPos, color = faceColor)
        }
    }

    if (outlineColor != null && !outlineColor.isTransparent) {
        drawCustomMesh(ClientRenderPipelines.Lines) { pose ->
            addShapeSideOutlines(pose.pose(), shape, side, hitPos, outlineColor)
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
    if (outerRadius <= 0f || outerColor.isTransparent && innerColor.isTransparent) {
        return
    }

    if (Mth.equal(innerOffset.lengthSquared(), 0f)) {
        val innerRatio = (innerRadius / outerRadius).coerceIn(0f, 1f)

        drawGradientCircleQuad(outerRadius, outerColor, innerColor, innerRatio, noDepthTest)
        return
    }

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

private fun WorldRenderEnvironment.drawGradientCircleQuad(
    radius: Float,
    outerColor: Color4b,
    innerColor: Color4b,
    innerRatio: Float,
    noDepthTest: Boolean,
) {
    fun packColorRG(color: Color4b): Int =
        ((color.r and 0xFF) shl 8) or (color.g and 0xFF)

    fun packColorBA(color: Color4b): Int =
        ((color.b and 0xFF) shl 8) or (color.a and 0xFF)

    val outerRg = packColorRG(outerColor)
    val outerBa = packColorBA(outerColor)
    val innerRg = packColorRG(innerColor)
    val innerBa = packColorBA(innerColor)

    drawCustomMesh(ClientRenderPipelines.gradientCircle(noDepthTest)) { matrix ->
        addVertex(matrix, -radius, 0f, -radius)
            .setUv(0f, 0f)
            .setUv1(outerRg, outerBa)
            .setUv2(innerRg, innerBa)
            .setLineWidth(innerRatio)
        addVertex(matrix, -radius, 0f, radius)
            .setUv(0f, 1f)
            .setUv1(outerRg, outerBa)
            .setUv2(innerRg, innerBa)
            .setLineWidth(innerRatio)
        addVertex(matrix, radius, 0f, radius)
            .setUv(1f, 1f)
            .setUv1(outerRg, outerBa)
            .setUv2(innerRg, innerBa)
            .setLineWidth(innerRatio)
        addVertex(matrix, radius, 0f, -radius)
            .setUv(1f, 0f)
            .setUv1(outerRg, outerBa)
            .setUv2(innerRg, innerBa)
            .setLineWidth(innerRatio)
    }
}

private fun WorldRenderEnvironment.drawRoundedRectQuad(
    radius: Float,
    argb: Int,
    noDepthTest: Boolean,
    uniform: GpuBufferSlice,
) {
    drawCustomMesh(
        pipeline = ClientRenderPipelines.roundedRect(noDepthTest),
        uniforms = objectObjectMapOf(ClientUniformDefine.ROUNDED_RECT.uboName, uniform),
    ) { pose ->
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
    if (radius <= 0f || color.isTransparent) {
        return
    }

    drawGradientCircleQuad(
        radius = radius,
        outerColor = color,
        innerColor = color,
        innerRatio = 0f,
        noDepthTest = true,
    )
}

/**
 * Function to draw the outline of a circle of the size [radius]
 *
 * @param radius The radius
 * @param color The color
 */
@JvmOverloads
fun WorldRenderEnvironment.drawCircleOutline(radius: Float, color: Color4b, noDepthTest: Boolean = true) {
    if (radius <= 0f || color.isTransparent) {
        return
    }

    drawRoundedRectQuad(
        radius = radius,
        argb = color.argb,
        noDepthTest = noDepthTest,
        uniform = ROUNDED_RECT_AS_OUTLINE_CIRCLE_UBO,
    )
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
