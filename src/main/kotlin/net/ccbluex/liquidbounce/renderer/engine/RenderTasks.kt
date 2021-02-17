/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.renderer.engine

import net.ccbluex.liquidbounce.utils.Mat4
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.nio.ByteBuffer

enum class OpenGLLevel(val minor: Int, val major: Int, val backendInfo: String) {
    OpenGL4_3(4, 3, "OpenGL 4.3+ (Multi rendering)"),
    OpenGL3_3(3, 3, "OpenGL 3.3+ (VAOs, VBOs, Instancing, Shaders)"),
    OpenGL1_2(1, 2, "OpenGL 1.2+ (Immediate mode, Display Lists)");

    /**
     * Determines if an OpenGL level is supported
     */
    fun isSupported(major: Int, minor: Int): Boolean {
        if (major > this.major)
            return true

        return major >= this.major && minor >= this.minor
    }

    fun supportsShaders(): Boolean = isSupported(3, 3)

    companion object {
        /**
         * Determines the best backend level for the given arguments
         */
        fun getBestLevelFor(major: Int, minor: Int): OpenGLLevel {
            return enumValues<OpenGLLevel>().first { it.isSupported(major, minor) }
        }
    }
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
     * Was this render task uploaded to VRAM?
     */
    private var uploaded = false
    var storageType = VBOStorageType.Stream

    /**
     * Can this render task render multiple
     *
     * @return Returns the batch renderer, if not supported, `null`
     */
    abstract fun getBatchRenderer(): BatchRenderer?

    /**
     * Sets up everything needed for rendering
     */
    abstract fun initRendering(level: OpenGLLevel, mvpMatrix: Mat4)

    /**
     * Executes the current render task. Always called after [initRendering] was called. Since some render tasks
     * can share their initialization methods, it is possible that not this instance's [initRendering] is called.
     */
    abstract fun draw(level: OpenGLLevel)

    /**
     * Calls [upload] if this function hasn't been called yet
     */
    fun uploadIfNotUploaded() {
        if (!this.uploaded) {
            this.upload()

            this.uploaded = true
        }
    }

    /**
     * Uploads the current state to VRAM
     */
    open fun upload() {}

    /**
     * Sets up everything needed for rendering.
     */
    abstract fun cleanupRendering(level: OpenGLLevel)

}

data class Point2f(val x: Float, val y: Float) {
    fun writeToBuffer(idx: Int, buffer: ByteBuffer) {
        buffer.putFloat(idx, x)
        buffer.putFloat(idx + 4, y)
    }
}

data class Vec4(val x: Float, val y: Float, val z: Float, val w: Float) {
    constructor(vec: Vec3, w: Float) : this(vec.x, vec.y, vec.z, w)
}

data class Vec3(val x: Float, val y: Float, val z: Float) {
    constructor(x: Double, y: Double, z: Double) : this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(vec: Vec3d) : this(vec.x, vec.y, vec.z)

    fun writeToBuffer(idx: Int, buffer: ByteBuffer) {
        buffer.putFloat(idx, x)
        buffer.putFloat(idx + 4, y)
        buffer.putFloat(idx + 8, z)
    }

    fun add(other: Vec3): Vec3 {
        return Vec3(this.x + other.x, this.y + other.y, this.z + other.z)
    }

    fun sub(other: Vec3): Vec3 {
        return Vec3(this.x - other.x, this.y - other.y, this.z - other.z)
    }

    operator fun plus(other: Vec3): Vec3 = add(other)
    operator fun minus(other: Vec3): Vec3 = sub(other)
    operator fun times(scale: Float): Vec3 = Vec3(this.x * scale, this.y * scale, this.z * scale)
}

/**
 * Contains a texture coordinate. The data gets normalized
 * `[0; 65535] -> [0.0f; 1.0f]`
 */
data class UV2s(val u: Short, val v: Short) {
    constructor(u: Float, v: Float) : this((u * 65535.0f).toInt().toShort(), (v * 65535.0f).toInt().toShort())

    fun writeToBuffer(idx: Int, buffer: ByteBuffer) {
        buffer.putShort(idx, u)
        buffer.putShort(idx + 2, v)
    }

    fun toFloatArray(): Array<Float> {
        return arrayOf((u.toInt() and 0xFFFF) / 65535.0f, (v.toInt() and 0xFFFF) / 65535.0f)
    }
}

data class Color4b(val r: Int, val g: Int, val b: Int, val a: Int) {
    companion object {
        val WHITE = Color4b(255, 255, 255, 255)
    }

    constructor(color: Color) : this(color.red, color.green, color.blue, color.alpha)

    fun writeToBuffer(idx: Int, buffer: ByteBuffer) {
        buffer.put(idx, r.toByte())
        buffer.put(idx + 1, g.toByte())
        buffer.put(idx + 2, b.toByte())
        buffer.put(idx + 3, a.toByte())
    }
}

class VAOData(storageType: VBOStorageType) {
    val arrayBuffer = VertexBufferObject(VBOTarget.ArrayBuffer, storageType)
    val elementBuffer = VertexBufferObject(VBOTarget.ElementArrayBuffer, storageType)
    val vao = VertexAttributeObject()

    fun bind() {
        this.vao.bind()
    }

    fun unbind() {
        this.vao.unbind()
    }
}

class InstancedVAOData(storageType: VBOStorageType) {
    val baseUploader = VAOData(storageType)
    val instanceData = VertexBufferObject(VBOTarget.ArrayBuffer, storageType)

    fun bind() {
        this.baseUploader.bind()
    }

    fun unbind() {
        this.baseUploader.unbind()
    }
}

enum class PrimitiveType(val verticesPerPrimitive: Int, val mode: Int) {
    /**
     * Lines; 2 vertices per primitive
     */
    Lines(2, GL11.GL_LINES),

    /**
     * Triangles; 3 vertices per primitive
     */
    Triangles(3, GL11.GL_TRIANGLES),

    /**
     * Points; 1 vertices per primitive
     */
    Points(1, GL11.GL_POINTS),

    /**
     * Triangle strip; 1 vertices per primitive
     */
    TriangleStrip(1, GL11.GL_TRIANGLE_STRIP),

    /**
     * Line loop; 1 vertices per primitive
     */
    LineLoop(1, GL11.GL_LINE_LOOP),
    LineStrip(1, GL11.GL_LINE_STRIP),
}
