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
package net.ccbluex.liquidbounce.utils

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20
import java.nio.FloatBuffer

class Mat4 {
    var a00 = 0f
    var a01 = 0f
    var a02 = 0f
    var a03 = 0f
    var a10 = 0f
    var a11 = 0f
    var a12 = 0f
    var a13 = 0f
    var a20 = 0f
    var a21 = 0f
    var a22 = 0f
    var a23 = 0f
    var a30 = 0f
    var a31 = 0f
    var a32 = 0f
    var a33 = 0f

    override fun toString(): String {
        return """Matrix4f:
$a00 $a01 $a02 $a03
$a10 $a11 $a12 $a13
$a20 $a21 $a22 $a23
$a30 $a31 $a32 $a33
"""
    }

    fun writeToBuffer(floatBuffer: FloatBuffer) {
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

    fun loadIdentity() {
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

    fun putToUniform(uniformLocation: Int) {
        val buffer = BufferUtils.createFloatBuffer(4 * 4)

        this.writeToBuffer(buffer)

        GL20.glUniformMatrix4fv(uniformLocation, false, buffer)
    }

    companion object {
        private inline fun pack(x: Int, y: Int): Int {
            return y * 4 + x
        }

        fun projectionMatrix(
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
    }

    init {
        loadIdentity()
    }
}
