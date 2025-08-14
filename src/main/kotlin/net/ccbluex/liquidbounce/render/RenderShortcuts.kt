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
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinDrawContextAccessor
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.engine.font.FontRendererBuffers
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gl.ShaderProgramKey
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.*
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11C
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
val HAS_AMD_VEGA_APU = GL11C.glGetString(GL11C.GL_RENDERER)?.startsWith("AMD Radeon(TM) RX Vega") ?: false &&
    GL11C.glGetString(GL11C.GL_VENDOR) == "ATI Technologies Inc."

val FULL_BOX = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
val EMPTY_BOX = Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

/**
 * Data class representing the rendering environment.
 *
 * @property matrixStack The matrix stack for rendering.
 */
sealed class RenderEnvironment(val matrixStack: MatrixStack) {
    val currentMvpMatrix: Matrix4f
        get() = matrixStack.peek().positionMatrix

    abstract fun relativeToCamera(pos: Vec3d): Vec3d

    inline fun withMatrixStack(block: MatrixStack.() -> Unit) = with(matrixStack) {
        push()
        try {
            block()
        } finally {
            pop()
        }
    }

    inline fun FontRenderer.withBuffers(block: FontRenderer.(FontRendererBuffers) -> Unit) {
        val fontBuffers = FontRendererBuffers()
        try {
            block(fontBuffers) // don't forget to `commit`!
        } finally {
            fontBuffers.draw()
        }
    }

    fun FontRenderer.commit(buffer: FontRendererBuffers) = commit(this@RenderEnvironment, buffer)
}

class GUIRenderEnvironment(matrixStack: MatrixStack) : RenderEnvironment(matrixStack) {
    override fun relativeToCamera(pos: Vec3d): Vec3d {
        return pos
    }
}

class WorldRenderEnvironment(matrixStack: MatrixStack, val camera: Camera) : RenderEnvironment(matrixStack) {
    override fun relativeToCamera(pos: Vec3d): Vec3d {
        return pos.subtract(camera.pos)
    }
}

fun newDrawContext(): DrawContext = DrawContext(mc, mc.bufferBuilders.entityVertexConsumers)

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

    RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    RenderSystem.disableBlend()
    RenderSystem.enableDepthTest()
    RenderSystem.enableCull()
    GL11C.glDisable(GL11C.GL_LINE_SMOOTH)
}

inline fun renderEnvironmentForGUI(matrixStack: MatrixStack = MatrixStack(), draw: GUIRenderEnvironment.() -> Unit) {
    RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR)
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    RenderSystem.enableBlend()

    draw(GUIRenderEnvironment(matrixStack))

    RenderSystem.disableBlend()
}

/**
 * Extension function to apply a position transformation to the current rendering environment.
 *
 * @param pos The position vector.
 * @param draw The block of code to be executed in the transformed environment.
 */
inline fun RenderEnvironment.withPosition(pos: Vec3, draw: RenderEnvironment.() -> Unit) {
    with(matrixStack) {
        push()
        translate(pos.x, pos.y, pos.z)
        try {
            draw()
        } finally {
            pop()
        }
    }
}

/**
 * Extension function to apply a position transformation to the current rendering environment.
 *
 * @param pos The position vector.
 * @param draw The block of code to be executed in the transformed environment.
 */
inline fun RenderEnvironment.withPosition(pos: Vec3d, draw: RenderEnvironment.() -> Unit) {
    with(matrixStack) {
        push()
        translate(pos.x, pos.y, pos.z)
        try {
            draw()
        } finally {
            pop()
        }
    }
}

/**
 * Shorthand for `withPosition(relativeToCamera(pos))`
 */
inline fun WorldRenderEnvironment.withPositionRelativeToCamera(pos: Vec3d, draw: WorldRenderEnvironment.() -> Unit) {
    val relativePos = relativeToCamera(pos)

    with(matrixStack) {
        push()
        translate(relativePos.x, relativePos.y, relativePos.z)
        try {
            draw()
        } finally {
            pop()
        }
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
 * Extension function to apply a color transformation to the current rendering environment.
 *
 * @param color4b The color transformation.
 * @param draw The block of code to be executed in the transformed environment.
 */
inline fun RenderEnvironment.withColor(color4b: Color4b, draw: RenderEnvironment.() -> Unit) {
    RenderSystem.setShaderColor(color4b.r / 255f, color4b.g / 255f, color4b.b / 255f, color4b.a / 255f)
    try {
        draw()
    } finally {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
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

/**
 * Function to draw lines using the specified [lines] vectors.
 *
 * @param lines The vectors representing the lines.
 */

fun RenderEnvironment.drawLines(vararg lines: Vec3) {
    drawLines(lines, mode = DrawMode.DEBUG_LINES)
}

/**
 * Function to draw a line strip using the specified [positions] vectors.
 *
 * @param positions The vectors representing the line strip.
 */
fun RenderEnvironment.drawLineStrip(vararg positions: Vec3) {
    drawLines(positions, mode = DrawMode.DEBUG_LINE_STRIP)
}

fun RenderEnvironment.drawLineStrip(positions: List<Vec3>) {
    drawLines(positions.toTypedArray(), mode = DrawMode.DEBUG_LINE_STRIP)
}

/**
 * Helper function to draw lines using the specified [lines] vectors and draw mode.
 *
 * @param lines The vectors representing the lines.
 * @param mode The draw mode for the lines.
 */
private fun RenderEnvironment.drawLines(lines: Array<out Vec3>, mode: DrawMode = DrawMode.DEBUG_LINES) {
    // If the array of lines is empty, we don't need to draw anything
    if (lines.isEmpty()) {
        return
    }

    val matrix = matrixStack.peek().positionMatrix
    val tessellator = RenderSystem.renderThreadTesselator()
    // Begin drawing lines with position format
    val buffer = tessellator.begin(mode, VertexFormats.POSITION)
    // Set the shader to the position program
    RenderSystem.setShader(ShaderProgramKeys.POSITION)

    // Draw the vertices of the box
    with(buffer) {
        // Draw the vertices of the box
        lines.forEach { (x, y, z) ->
            vertex(matrix, x, y, z)
        }

        // Draw the outlined box
        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }
}

/**
 */
fun RenderEnvironment.drawTextureQuad(pos1: Vec3d, pos2: Vec3d) {
    val tessellator = RenderSystem.renderThreadTesselator()
    // Begin drawing lines with position format
    val buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
    RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR)

    val matrix = matrixStack.peek().positionMatrix

    // Draw the vertices of the box
    with(buffer) {
        vertex(matrix, pos1.x.toFloat(), pos2.y.toFloat(), 0.0F)
            .texture(0f, 1.0F)
            .color(255, 255, 255, 255)

        vertex(matrix, pos2.x.toFloat(), pos2.y.toFloat(), 0.0F)
            .texture(1.0F, 1.0F)
            .color(255, 255, 255, 255)

        vertex(matrix, pos2.x.toFloat(), pos1.y.toFloat(), 0.0F)
            .texture(1.0F, 0.0f)
            .color(255, 255, 255, 255)

        vertex(matrix, pos1.x.toFloat(), pos1.y.toFloat(), 0.0F)
            .texture(0.0f, 0.0f)
            .color(255, 255, 255, 255)

        // Draw the outlined box
        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }
}

/**
 */
inline fun RenderEnvironment.drawCustomMesh(
    drawMode: DrawMode,
    vertexFormat: VertexFormat,
    shader: ShaderProgramKey,
    drawer: BufferBuilder.(Matrix4f) -> Unit
) {
    val tessellator = RenderSystem.renderThreadTesselator()
    val buffer = tessellator.begin(drawMode, vertexFormat)

    RenderSystem.setShader(shader)

    val matrix = matrixStack.peek().positionMatrix

    // Draw the vertices of the box
    with(buffer) {
        // Begin drawing lines with position format

        drawer(this, matrix)

        // Draw the custom mesh
        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }
}

fun RenderEnvironment.drawQuad(pos1: Vec3, pos2: Vec3) {
    val tessellator = RenderSystem.renderThreadTesselator()
    // Begin drawing lines with position format
    val buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION)

    RenderSystem.setShader(ShaderProgramKeys.POSITION)

    val matrix = matrixStack.peek().positionMatrix

    // Draw the vertices of the box
    with(buffer) {
        vertex(matrix, pos1.x, pos2.y, pos1.z)
        vertex(matrix, pos2.x, pos2.y, pos2.z)
        vertex(matrix, pos2.x, pos1.y, pos2.z)
        vertex(matrix, pos1.x, pos1.y, pos1.z)

        // Draw the outlined box
        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }
}

fun RenderEnvironment.drawQuadOutlines(pos1: Vec3, pos2: Vec3) {
    val tessellator = RenderSystem.renderThreadTesselator()
    // Begin drawing lines with position format
    val buffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION)

    RenderSystem.setShader(ShaderProgramKeys.POSITION)

    val matrix = matrixStack.peek().positionMatrix

    // Draw the vertices of the box
    with(buffer) {

        vertex(matrix, pos1.x, pos1.y, pos1.z)
        vertex(matrix, pos1.x, pos2.y, pos1.z)

        vertex(matrix, pos1.x, pos2.y, pos1.z)
        vertex(matrix, pos2.x, pos2.y, pos1.z)

        vertex(matrix, pos2.x, pos1.y, pos1.z)
        vertex(matrix, pos2.x, pos2.y, pos1.z)

        vertex(matrix, pos1.x, pos1.y, pos1.z)
        vertex(matrix, pos2.x, pos1.y, pos1.z)

        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }
}

fun RenderEnvironment.drawTriangle(p1: Vec3, p2: Vec3, p3: Vec3) {
    val tessellator = RenderSystem.renderThreadTesselator()
    // Begin drawing lines with position format
    val bufferBuilder = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION)

    RenderSystem.setShader(ShaderProgramKeys.POSITION)

    val matrix = matrixStack.peek().positionMatrix

    // Draw the vertices of the box
    with(bufferBuilder) {
        vertex(matrix, p1.x, p1.y, p1.z)
        vertex(matrix, p2.x, p2.y, p2.z)
        vertex(matrix, p3.x, p3.y, p3.z)

        // Draw the outlined box
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
    }
}

fun BufferBuilder.coloredTriangle(matrix: Matrix4f, p1: Vec3d, p2: Vec3d, p3: Vec3d, color4b: Color4b) {
    vertex(matrix, p1.x.toFloat(), p1.y.toFloat(), p1.z.toFloat()).color(color4b.toARGB())
    vertex(matrix, p2.x.toFloat(), p2.y.toFloat(), p2.z.toFloat()).color(color4b.toARGB())
    vertex(matrix, p3.x.toFloat(), p3.y.toFloat(), p3.z.toFloat()).color(color4b.toARGB())
}

/**
 * Function to draw a side box using the specified [box] and [side].
 *
 * @param box The bounding box of the side.
 * @param side The direction of the side.
 * @param onlyOutline Determines if the function only should draw the outline of the [side] or only fill it in
 */
@Suppress("LongMethod")
fun RenderEnvironment.drawSideBox(box: Box, side: Direction, onlyOutline: Boolean = false) {
    val matrix = matrixStack.peek().positionMatrix
    val tessellator = RenderSystem.renderThreadTesselator()

    // Begin drawing lines or quads with position format
    val buffer = tessellator.begin(
        if (onlyOutline) {
            DrawMode.DEBUG_LINE_STRIP
        } else {
            DrawMode.QUADS
        },
        VertexFormats.POSITION
    )

    // Set the shader to the position program
    RenderSystem.setShader(ShaderProgramKeys.POSITION)

    // Draw the vertices of the box
    with(buffer) {
        // Draw the vertices of the box
        val vertices = getVerticesForSide(box, side)

        vertices.forEach { (x, y, z) ->
            vertex(matrix, x, y, z)
        }

        if (onlyOutline) {
            vertex(matrix, vertices[0].x, vertices[0].y, vertices[0].z)
        }

        // Draw the outlined box
        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }
}

fun RenderEnvironment.drawBoxSide(box: Box, side: Direction, face: Color4b, outline: Color4b){
    val matrix = matrixStack.peek().positionMatrix
    val tessellator = RenderSystem.renderThreadTesselator()

    // Set the shader to the position program
    RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

    val vertices = getVerticesForSide(box, side)

    var buffer = tessellator.begin(
        DrawMode.QUADS,
        VertexFormats.POSITION_COLOR
    )

    // Draw the vertices of the box
    with(buffer) {
        vertices.forEach { (x, y, z) ->
            vertex(matrix, x, y, z).color(face.r, face.g, face.b, face.a)
        }

        // Draw the outlined box
        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }

    if (outline.a == 0) {
        return
    }

    buffer = tessellator.begin(
        DrawMode.DEBUG_LINE_STRIP,
        VertexFormats.POSITION_COLOR
    )

    // Draw the vertices of the box
    with(buffer) {
        vertices.forEach { (x, y, z) ->
            vertex(matrix, x, y, z).color(outline.r, outline.g, outline.b, outline.a)
        }

        // close the loop
        val firstVertex = vertices[0]
        vertex(matrix, firstVertex.x, firstVertex.y, firstVertex.z).color(outline.r, outline.g, outline.b, outline.a)

        // Draw the outlined box
        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }
}

private fun getVerticesForSide(box: Box, side: Direction) = when (side) {
    Direction.DOWN -> arrayOf(
        Vec3(box.minX, box.minY, box.maxZ),
        Vec3(box.minX, box.minY, box.minZ),
        Vec3(box.maxX, box.minY, box.minZ),
        Vec3(box.maxX, box.minY, box.maxZ)
    )

    Direction.UP -> arrayOf(
        Vec3(box.minX, box.maxY, box.minZ),
        Vec3(box.minX, box.maxY, box.maxZ),
        Vec3(box.maxX, box.maxY, box.maxZ),
        Vec3(box.maxX, box.maxY, box.minZ)
    )

    Direction.NORTH -> arrayOf(
        Vec3(box.maxX, box.maxY, box.minZ),
        Vec3(box.maxX, box.minY, box.minZ),
        Vec3(box.minX, box.minY, box.minZ),
        Vec3(box.minX, box.maxY, box.minZ)
    )

    Direction.SOUTH -> arrayOf(
        Vec3(box.minX, box.maxY, box.maxZ),
        Vec3(box.minX, box.minY, box.maxZ),
        Vec3(box.maxX, box.minY, box.maxZ),
        Vec3(box.maxX, box.maxY, box.maxZ)
    )

    Direction.WEST -> arrayOf(
        Vec3(box.minX, box.maxY, box.minZ),
        Vec3(box.minX, box.minY, box.minZ),
        Vec3(box.minX, box.minY, box.maxZ),
        Vec3(box.minX, box.maxY, box.maxZ)
    )

    Direction.EAST -> arrayOf(
        Vec3(box.maxX, box.maxY, box.maxZ),
        Vec3(box.maxX, box.minY, box.maxZ),
        Vec3(box.maxX, box.minY, box.minZ),
        Vec3(box.maxX, box.maxY, box.minZ)
    )
}

/**
 * Function to render a gradient quad using specified [vertices] and [colors]
 *
 * @param vertices The four vectors to draw the quad
 * @param colors The colors for the vertices
 */
fun RenderEnvironment.drawGradientQuad(vertices: List<Vec3>, colors: List<Color4b>) {
    require(vertices.size == colors.size) { "there must be a color for every vertex" }
    require(vertices.size % 4 == 0) { "vertices must be dividable by 4" }
    val matrix = matrixStack.peek().positionMatrix
    val tessellator = RenderSystem.renderThreadTesselator()
    val buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR)

    // Set the shader to the position program
    RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

    with(buffer) {
        vertices.forEachIndexed { index, (x, y, z) ->
            val color4b = colors[index]
            vertex(matrix, x, y, z).color(color4b.toARGB())
        }

        // Draw the quad
        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }
}

const val CIRCLE_RES = 40

// using a val instead of a function for better performance
val circlePoints =
    (0..CIRCLE_RES).map {
        val theta = 2 * PI * it / CIRCLE_RES
        Vec3(cos(theta), 0.0, sin(theta))
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
    innerOffset: Vec3 = Vec3(0f, 0f, 0f)
) {

    val matrix = matrixStack.peek().positionMatrix
    val tessellator = RenderSystem.renderThreadTesselator()
    val buffer = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR)

    // Set the shader to the position and color program
    RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

    with(buffer) {
        for (p in circlePoints) {
            val outerP = p * outerRadius
            val innerP = p * innerRadius + innerOffset

            vertex(matrix, outerP.x, outerP.y, outerP.z)
                .color(outerColor4b.toARGB())
            vertex(matrix, innerP.x, innerP.y, innerP.z)
                .color(innerColor4b.toARGB())
        }

        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }
}

/**
 * Function to draw the outline of a circle of the size [radius]
 *
 * @param radius The radius
 * @param color4b The color
 */
fun RenderEnvironment.drawCircleOutline(radius: Float, color4b: Color4b) {
    val matrix = matrixStack.peek().positionMatrix
    val tessellator = RenderSystem.renderThreadTesselator()
    val buffer = tessellator.begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR)

    // Set the shader to the position and color program
    RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

    with(buffer) {
        for (p in circlePoints) {
            val point = p * radius

            vertex(matrix, point.x, point.y, point.z)
                .color(color4b.toARGB())
        }

        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }
}

/**
 * Function to draw an outlined box using the specified [box].
 *
 * @param box The bounding box of the box.
 */
fun RenderEnvironment.drawOutlinedBox(box: Box) {
    val matrix = matrixStack.peek().positionMatrix
    val tessellator = RenderSystem.renderThreadTesselator()
    // Begin drawing lines with position format
    val buffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION)

    // Set the shader to the position program
    RenderSystem.setShader(ShaderProgramKeys.POSITION)

    // Draw the vertices of the box
    with(buffer) {
        // Draw the vertices of the box
        val vertices = listOf(
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ)
        )

        vertices.forEach { (x, y, z) ->
            vertex(matrix, x, y, z)
        }

        // Draw the outlined box
        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }
}

/**
 * Function to draw a solid box using the specified [box].
 *
 * @param box The bounding box of the box.
 */
fun RenderEnvironment.drawSolidBox(box: Box) {
    val matrix = matrixStack.peek().positionMatrix
    val tessellator = RenderSystem.renderThreadTesselator()
    // Begin drawing lines with position format
    val buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION)

    // Set the shader to the position program
    RenderSystem.setShader(ShaderProgramKeys.POSITION)

    // Begin drawing quads with position format

    with(buffer) {
        // Draw the vertices of the box
        val vertices = listOf(
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ)
        )

        vertices.forEach { (x, y, z) ->
            vertex(matrix, x, y, z)
        }

        // Draw the solid box
        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
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
