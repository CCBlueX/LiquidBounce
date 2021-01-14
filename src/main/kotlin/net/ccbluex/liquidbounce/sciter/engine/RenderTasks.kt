package net.ccbluex.liquidbounce.renderer.engine

import net.ccbluex.liquidbounce.renderer.engine.ColoredPrimitiveRenderTask.PrimitiveType
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

enum class OpenGLLevel {
    OpenGL1_2,
    OpenGL2_1,
    OpenGL4_1
}

/**
 * Used to draw multiple render tasks at once
 */
abstract class BatchRenderer

/**
 * A super class of structs that can be fed to the render engine.
 */
abstract class RenderTask {
    /**
     * Can this render task render multiple
     *
     * @return Returns the batch renderer, if not supported, `null`
     */
    abstract fun getBatchRenderer(): BatchRenderer?

    /**
     * Sets up everything needed for rendering
     */
    abstract fun initRendering(level: OpenGLLevel)

    /**
     * Executes the current render task. Always called after [initRendering] was called. Since some render tasks
     * can share their initialization methods, it is possible that not this instance's [initRendering] is called.
     */
    abstract fun draw(level: OpenGLLevel)

    /**
     * Sets up everything needed for rendering.
     */
    abstract fun cleanupRendering(level: OpenGLLevel)

    /**
     * Render tasks with the same type identifier can share their initialization, cleanup and rendering functions.
     */
    abstract fun typeId(): Int
}

data class Point2D(val x: Float, val y: Float) {
    inline fun writeToFloatBuffer(idx: Int, buffer: FloatBuffer) {
        buffer.put(idx, x)
        buffer.put(idx + 1, y)
    }
}

data class Color4b(val r: Int, val g: Int, val b: Int, val a: Int) {
    constructor(color: Color) : this(color.red, color.green, color.blue, color.alpha)

    inline fun writeToIntBuffer(idx: Int, buffer: IntBuffer) {
        buffer.put(
            idx, (a and 0xFF shl 24 or
                (r and 0xFF shl 16) or
                (g and 0xFF shl 8) or
                (b and 0xFF shl 0))
        )
    }
}

data class Point3D(val x: Float, val y: Float, val z: Float)

/**
 * Colored Primitive Render Task
 *
 * Can draw colored lines and triangles in 2 dimensions.
 *
 * If you want to render quads, use [PrimitiveType.Triangle] as [type] and set [maxPrimitiveCount] to twice the count of quads
 */
class ColoredPrimitiveRenderTask(private val maxPrimitiveCount: Int, private val type: PrimitiveType) : RenderTask() {
    /**
     * The buffer, the vertices are stored in.
     */
    private val vertexBuffer: ByteBuffer = BufferUtils.createByteBuffer(vertexIndex(maxPrimitiveCount) * 4)

    /**
     * The count of primitives that are stored in [vertexBuffer]
     */
    private var primitiveCount: Int = 0

    /**
     * Renders a line. Only available if [type] is [PrimitiveType.Lines].
     *
     * @throws IllegalStateException If this tasks doesn't render lines ([type] != [PrimitiveType.Lines])
     * @throws IllegalStateException If the maximal vertex count is reached ([primitiveCount] >= [maxPrimitiveCount])
     */
    fun line(p1: Point2D, p2: Point2D, color1: Color4b, color2: Color4b = color1) {
        if (this.type != PrimitiveType.Lines)
            throw IllegalStateException("Type is not Lines")

        val currentIndex = vertexIndex(primitiveCount)

        val intBuffer = this.vertexBuffer.asIntBuffer()
        val floatBuffer = this.vertexBuffer.asFloatBuffer()

        p1.writeToFloatBuffer(currentIndex, floatBuffer)
        color1.writeToIntBuffer(currentIndex + 2, intBuffer)
        p2.writeToFloatBuffer(currentIndex + 3, floatBuffer)
        color2.writeToIntBuffer(currentIndex + 5, intBuffer)

        primitiveCount++
    }

    /**
     * Renders a triangle. Only available if [type] is [PrimitiveType.Triangle].
     *
     * @throws IllegalStateException If this tasks doesn't render lines ([type] != [PrimitiveType.Triangle])
     * @throws IllegalStateException If the maximal vertex count is reached ([primitiveCount] >= [maxPrimitiveCount])
     */
    fun triangle(
        p1: Point2D,
        p2: Point2D,
        p3: Point2D,
        color1: Color4b,
        color2: Color4b = color1,
        color3: Color4b = color1
    ) {
        if (this.type != PrimitiveType.Triangle)
            throw IllegalStateException("Type is not Triangles")

        val currentIndex = vertexIndex(primitiveCount)

        val intBuffer = this.vertexBuffer.asIntBuffer()
        val floatBuffer = this.vertexBuffer.asFloatBuffer()

        p1.writeToFloatBuffer(currentIndex, floatBuffer)
        color1.writeToIntBuffer(currentIndex + 2, intBuffer)
        p2.writeToFloatBuffer(currentIndex + 3, floatBuffer)
        color2.writeToIntBuffer(currentIndex + 5, intBuffer)
        p3.writeToFloatBuffer(currentIndex + 6, floatBuffer)
        color3.writeToIntBuffer(currentIndex + 8, intBuffer)

        primitiveCount++
    }

    /**
     * The vertex count for a given count of primitives.
     *
     * `Two dimensions * Vertices per primitive * (1 int of color + 2 dimensions) * primitives`
     */
    private fun vertexIndex(primitives: Int) = primitives * type.verticesPerPrimitive * (1 + 2)

    override fun getBatchRenderer(): BatchRenderer? = null

    override fun initRendering(level: OpenGLLevel) {

    }

    override fun draw(level: OpenGLLevel) {
        when (level) {
            // Use Immediate mode for OpenGL 1.2
            OpenGLLevel.OpenGL1_2 -> {
                // Begin rendering with the type's mode
                GL11.glBegin(this.type.mode)

                val floatBuffer = this.vertexBuffer.asFloatBuffer()
                val intBuffer = this.vertexBuffer.asIntBuffer()

                val vpp = this.type.verticesPerPrimitive

                // Iterate through the primitives
                for (i in 0 until this.primitiveCount) {
                    // Draw the vertices of the current primitive
                    for (j in 0 until vpp) {
                        val idx = vertexIndex(i) + j * 3

                        val color = intBuffer[idx + 2]

                        // Set the vertex color
                        GL11.glColor4f(
                            ((color shr 8) and 255) / 255.0f,
                            ((color shr 16) and 255) / 255.0f,
                            (color and 255) / 255.0f,
                            ((color shr 24) and 255) / 255.0f
                        )
                        // Set the vertex position
                        GL11.glVertex2f(floatBuffer[idx], floatBuffer[idx + 1])
                    }
                }

                // Finish drawing
                GL11.glEnd()
            }
            // Use VBOs for later OpenGL versions
            OpenGLLevel.OpenGL2_1, OpenGLLevel.OpenGL4_1 -> {

            }
        }
    }

    override fun cleanupRendering(level: OpenGLLevel) {

    }

    /**
     * Render tasks can *always* be packed into a single VBO.
     */
    override fun typeId(): Int = 0

    enum class PrimitiveType(val verticesPerPrimitive: Int, val mode: Int) {
        /**
         * Lines; 2 vertices per primitive
         */
        Lines(2, GL11.GL_LINES),

        /**
         * Triangles; 3 vertices per primitive
         */
        Triangle(3, GL11.GL_TRIANGLES),

        /**
         * Quads. Tessellates the quad into two triangles; 4 vertices per primitive
         */
        Quads(6, GL11.GL_TRIANGLE_STRIP)
    }
}
