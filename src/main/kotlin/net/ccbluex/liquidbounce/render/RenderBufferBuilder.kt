@file:Suppress("LongParameterList")

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.UV2f
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.minecraft.client.gl.ShaderProgram
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

/**
 * A utility class for drawing shapes in batches.
 *
 * Not sync, not send. Not thread-safe at all.
 */
class RenderBufferBuilder<I: VertexInputType>(
    private val drawMode: DrawMode,
    private val vertexFormat: I,
    private val tesselator: Tessellator
) {
    val bufferBuilder: BufferBuilder = tesselator.buffer

    init {
        // Begin drawing lines with position format
        bufferBuilder.begin(drawMode, vertexFormat.vertexFormat)
    }

    /**
     * Function to draw a solid box using the specified [box].
     *
     * @param box The bounding box of the box.
     */
    fun drawBox(env: RenderEnvironment, box: Box) {
        val matrix = env.currentMvpMatrix

        // Draw the vertices of the box
        boxVertexPositions(box).forEach { (x, y, z) ->
            bufferBuilder.vertex(matrix, x, y, z).next()
        }
    }

    private fun boxVertexPositions(box: Box): List<Vec3> {
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
        return vertices
    }

    fun draw() {
        if (bufferBuilder.isBatchEmpty) {
            tesselator.buffer.end()

            return
        }

        RenderSystem.setShader { vertexFormat.shaderProgram }

        tesselator.draw()
        tesselator.buffer.reset()
    }

    companion object {
        val TESSELATOR_A: Tessellator = Tessellator(0x200000)
        val TESSELATOR_B: Tessellator = Tessellator(0x200000)
        val TESSELATOR_C: Tessellator = Tessellator(0x200000)
    }

}

fun RenderBufferBuilder<VertexInputType.PosTexColor>.drawQuad(
    env: RenderEnvironment,
    pos1: Vec3d,
    uv1: UV2f,
    pos2: Vec3d,
    uv2: UV2f,
    color: Color4b
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(bufferBuilder) {
        vertex(matrix, pos1.x.toFloat(), pos2.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv2.v)
            .color(color.toRGBA())
            .next()
        vertex(matrix, pos2.x.toFloat(), pos2.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv2.v)
            .color(color.toRGBA())
            .next()
        vertex(matrix, pos2.x.toFloat(), pos1.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv1.v)
            .color(color.toRGBA())
            .next()
        vertex(matrix, pos1.x.toFloat(), pos1.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv1.v)
            .color(color.toRGBA())
            .next()
    }
}

fun RenderBufferBuilder<VertexInputType.Pos>.drawQuad(
    env: RenderEnvironment,
    pos1: Vec3,
    pos2: Vec3,
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(bufferBuilder) {
        vertex(matrix, pos1.x, pos2.y, pos1.z).next()
        vertex(matrix, pos2.x, pos2.y, pos2.z).next()
        vertex(matrix, pos2.x, pos1.y, pos2.z).next()
        vertex(matrix, pos1.x, pos1.y, pos1.z).next()
    }
}

fun RenderBufferBuilder<VertexInputType.Pos>.drawQuadOutlines(
    env: RenderEnvironment,
    pos1: Vec3,
    pos2: Vec3,
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(bufferBuilder) {
        vertex(matrix, pos1.x, pos1.y, pos1.z).next()
        vertex(matrix, pos1.x, pos2.y, pos1.z).next()

        vertex(matrix, pos1.x, pos2.y, pos1.z).next()
        vertex(matrix, pos2.x, pos2.y, pos1.z).next()

        vertex(matrix, pos2.x, pos1.y, pos1.z).next()
        vertex(matrix, pos2.x, pos2.y, pos1.z).next()

        vertex(matrix, pos1.x, pos1.y, pos1.z).next()
        vertex(matrix, pos2.x, pos1.y, pos1.z).next()
    }
}

fun RenderBufferBuilder<VertexInputType.PosColor>.drawLine(
    env: RenderEnvironment,
    pos1: Vec3,
    pos2: Vec3,
    color: Color4b
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(bufferBuilder) {
        vertex(matrix, pos1.x, pos1.y, pos1.z).color(color.toRGBA()).next()
        vertex(matrix, pos2.x, pos2.y, pos2.z).color(color.toRGBA()).next()
    }
}

sealed class VertexInputType {
    abstract val vertexFormat: VertexFormat
    abstract val shaderProgram: ShaderProgram

    object Pos : VertexInputType() {
        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION
        override val shaderProgram: ShaderProgram
            get() = GameRenderer.getPositionProgram()!!
    }
    object PosColor : VertexInputType() {
        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION_COLOR
        override val shaderProgram: ShaderProgram
            get() = GameRenderer.getPositionColorProgram()!!
    }
    object PosTexColor : VertexInputType() {
        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION_TEXTURE_COLOR
        override val shaderProgram: ShaderProgram
            get() = GameRenderer.getPositionTexColorProgram()!!
    }
}
