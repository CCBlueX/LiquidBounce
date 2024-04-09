/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.math

import net.ccbluex.liquidbounce.render.engine.Vec4
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20
import java.nio.FloatBuffer

inline fun Matrix4f.toMat4(): Mat4 {
    return Mat4(this)
}

class Mat4() {
    var a00 = 0f
    private var a01 = 0f
    private var a02 = 0f
    var a03 = 0f
    private var a10 = 0f
    var a11 = 0f
    private var a12 = 0f
    var a13 = 0f
    private var a20 = 0f
    private var a21 = 0f
    var a22 = 0f
    var a23 = 0f
    private var a30 = 0f
    private var a31 = 0f
    private var a32 = 0f
    var a33 = 0f

    constructor(quaternion: Quaternionf) : this() {
        val f = quaternion.x
        val g = quaternion.y
        val h = quaternion.z
        val i = quaternion.w
        val j = 2.0f * f * f
        val k = 2.0f * g * g
        val l = 2.0f * h * h
        a00 = 1.0f - k - l
        a11 = 1.0f - l - j
        a22 = 1.0f - j - k
        a33 = 1.0f
        val m = f * g
        val n = g * h
        val o = h * f
        val p = f * i
        val q = g * i
        val r = h * i
        a10 = 2.0f * (m + r)
        a01 = 2.0f * (m - r)
        a20 = 2.0f * (o - q)
        a02 = 2.0f * (o + q)
        a21 = 2.0f * (n + p)
        a12 = 2.0f * (n - p)
    }

    constructor(matrix4f: Matrix4f) : this() {
        this.a00 = matrix4f.m00()
        this.a01 = matrix4f.m01()
        this.a02 = matrix4f.m02()
        this.a03 = matrix4f.m03()
        this.a10 = matrix4f.m10()
        this.a11 = matrix4f.m11()
        this.a12 = matrix4f.m12()
        this.a13 = matrix4f.m13()
        this.a20 = matrix4f.m20()
        this.a21 = matrix4f.m21()
        this.a22 = matrix4f.m22()
        this.a23 = matrix4f.m23()
        this.a30 = matrix4f.m30()
        this.a31 = matrix4f.m31()
        this.a32 = matrix4f.m32()
        this.a33 = matrix4f.m33()
    }

    constructor(mat4: Mat4) : this() {
        this.a00 = mat4.a00
        this.a01 = mat4.a01
        this.a02 = mat4.a02
        this.a03 = mat4.a03
        this.a10 = mat4.a10
        this.a11 = mat4.a11
        this.a12 = mat4.a12
        this.a13 = mat4.a13
        this.a20 = mat4.a20
        this.a21 = mat4.a21
        this.a22 = mat4.a22
        this.a23 = mat4.a23
        this.a30 = mat4.a30
        this.a31 = mat4.a31
        this.a32 = mat4.a32
        this.a33 = mat4.a33
    }

    override fun toString(): String {
        return """Mat4:
$a00 $a01 $a02 $a03
$a10 $a11 $a12 $a13
$a20 $a21 $a22 $a23
$a30 $a31 $a32 $a33
"""
    }

    private fun writeToBuffer(floatBuffer: FloatBuffer) {
        floatBuffer.put(pack(0, 0), a00)
        floatBuffer.put(pack(0, 1), a01)
        floatBuffer.put(pack(0, 2), a02)
        floatBuffer.put(pack(0, 3), a03)
        floatBuffer.put(pack(1, 0), a10)
        floatBuffer.put(pack(1, 1), a11)
        floatBuffer.put(pack(1, 2), a12)
        floatBuffer.put(pack(1, 3), a13)
        floatBuffer.put(pack(2, 0), a20)
        floatBuffer.put(pack(2, 1), a21)
        floatBuffer.put(pack(2, 2), a22)
        floatBuffer.put(pack(2, 3), a23)
        floatBuffer.put(pack(3, 0), a30)
        floatBuffer.put(pack(3, 1), a31)
        floatBuffer.put(pack(3, 2), a32)
        floatBuffer.put(pack(3, 3), a33)
    }

    fun toArray(): FloatArray {
        val array = FloatArray(16)

        array[pack(0, 0)] = a00
        array[pack(0, 1)] = a01
        array[pack(0, 2)] = a02
        array[pack(0, 3)] = a03
        array[pack(1, 0)] = a10
        array[pack(1, 1)] = a11
        array[pack(1, 2)] = a12
        array[pack(1, 3)] = a13
        array[pack(2, 0)] = a20
        array[pack(2, 1)] = a21
        array[pack(2, 2)] = a22
        array[pack(2, 3)] = a23
        array[pack(3, 0)] = a30
        array[pack(3, 1)] = a31
        array[pack(3, 2)] = a32
        array[pack(3, 3)] = a33

        return array
    }

    private fun loadIdentity() {
        a00 = 1.0f
        a01 = 0.0f
        a02 = 0.0f
        a03 = 0.0f
        a10 = 0.0f
        a11 = 1.0f
        a12 = 0.0f
        a13 = 0.0f
        a20 = 0.0f
        a21 = 0.0f
        a22 = 1.0f
        a23 = 0.0f
        a30 = 0.0f
        a31 = 0.0f
        a32 = 0.0f
        a33 = 1.0f
    }

    fun multiply(matrix: Mat4) {
        val f = a00 * matrix.a00 + a01 * matrix.a10 + a02 * matrix.a20 + a03 * matrix.a30
        val g = a00 * matrix.a01 + a01 * matrix.a11 + a02 * matrix.a21 + a03 * matrix.a31
        val h = a00 * matrix.a02 + a01 * matrix.a12 + a02 * matrix.a22 + a03 * matrix.a32
        val i = a00 * matrix.a03 + a01 * matrix.a13 + a02 * matrix.a23 + a03 * matrix.a33
        val j = a10 * matrix.a00 + a11 * matrix.a10 + a12 * matrix.a20 + a13 * matrix.a30
        val k = a10 * matrix.a01 + a11 * matrix.a11 + a12 * matrix.a21 + a13 * matrix.a31
        val l = a10 * matrix.a02 + a11 * matrix.a12 + a12 * matrix.a22 + a13 * matrix.a32
        val m = a10 * matrix.a03 + a11 * matrix.a13 + a12 * matrix.a23 + a13 * matrix.a33
        val n = a20 * matrix.a00 + a21 * matrix.a10 + a22 * matrix.a20 + a23 * matrix.a30
        val o = a20 * matrix.a01 + a21 * matrix.a11 + a22 * matrix.a21 + a23 * matrix.a31
        val p = a20 * matrix.a02 + a21 * matrix.a12 + a22 * matrix.a22 + a23 * matrix.a32
        val q = a20 * matrix.a03 + a21 * matrix.a13 + a22 * matrix.a23 + a23 * matrix.a33
        val r = a30 * matrix.a00 + a31 * matrix.a10 + a32 * matrix.a20 + a33 * matrix.a30
        val s = a30 * matrix.a01 + a31 * matrix.a11 + a32 * matrix.a21 + a33 * matrix.a31
        val t = a30 * matrix.a02 + a31 * matrix.a12 + a32 * matrix.a22 + a33 * matrix.a32
        val u = a30 * matrix.a03 + a31 * matrix.a13 + a32 * matrix.a23 + a33 * matrix.a33
        a00 = f
        a01 = g
        a02 = h
        a03 = i
        a10 = j
        a11 = k
        a12 = l
        a13 = m
        a20 = n
        a21 = o
        a22 = p
        a23 = q
        a30 = r
        a31 = s
        a32 = t
        a33 = u
    }

    operator fun times(vec: Vec4): Vec4 {
        val x = this.a00 * vec.x + this.a01 * vec.y + this.a02 * vec.z + this.a03 * vec.w
        val y = this.a10 * vec.x + this.a11 * vec.y + this.a12 * vec.z + this.a13 * vec.w
        val z = this.a20 * vec.x + this.a21 * vec.y + this.a22 * vec.z + this.a23 * vec.w
        val w = this.a30 * vec.x + this.a31 * vec.y + this.a32 * vec.z + this.a33 * vec.w

        return Vec4(x, y, z, w)
    }

    fun multiply(quaternion: Quaternionf) {
        this.multiply(Mat4(quaternion))
    }

    fun putToUniform(uniformLocation: Int) {
        val buffer = BufferUtils.createFloatBuffer(4 * 4)

        this.writeToBuffer(buffer)

        GL20.glUniformMatrix4fv(uniformLocation, false, buffer)
    }

    fun toBuffer(): FloatBuffer {
        val floatBuffer = BufferUtils.createFloatBuffer(16)

        this.writeToBuffer(floatBuffer)

        return floatBuffer
    }

    companion object {
        private inline fun pack(x: Int, y: Int): Int {
            return y * 4 + x
        }

        fun orthograpicProjectionMatrix(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            nearPlane: Float,
            farPlane: Float
        ): Mat4 {
            val mat4 = Mat4()
            mat4.a00 = 2.0f / (right - left)
            mat4.a11 = 2.0f / (top - bottom)
            val f = farPlane - nearPlane
            mat4.a22 = -2.0f / f
            mat4.a03 = -(right + left) / (right - left)
            mat4.a13 = -(top + bottom) / (top - bottom)
            mat4.a23 = -(farPlane + nearPlane) / f
            mat4.a33 = 1.0f
            return mat4
        }

        fun translate(x: Float, y: Float, z: Float): Mat4 {
            val mat4 = Mat4()
            mat4.a00 = 1.0f
            mat4.a11 = 1.0f
            mat4.a22 = 1.0f
            mat4.a33 = 1.0f
            mat4.a03 = x
            mat4.a13 = y
            mat4.a23 = z
            return mat4
        }

        fun scale(x: Float, y: Float, z: Float): Mat4 {
            val mat4 = Mat4()

            mat4.a00 = x
            mat4.a11 = y
            mat4.a22 = z
            mat4.a33 = 1.0f

            return mat4
        }
    }

    init {
        loadIdentity()
    }
}
