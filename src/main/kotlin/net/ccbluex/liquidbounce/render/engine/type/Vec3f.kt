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
package net.ccbluex.liquidbounce.render.engine.type

import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.minecraft.util.math.Position
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

@JvmRecord
data class Vec3f(val x: Float, val y: Float, val z: Float) {
    constructor(x: Double, y: Double, z: Double) : this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(vec: Position) : this(vec.x, vec.y, vec.z)
    constructor(vec: Vec3i) : this(vec.x.toFloat(), vec.y.toFloat(), vec.z.toFloat())

    fun add(x: Float, y: Float, z: Float): Vec3f {
        return Vec3f(this.x + x, this.y + y, this.z + z)
    }

    fun add(other: Vec3f): Vec3f = add(other.x, other.y, other.z)

    private fun sub(other: Vec3f): Vec3f {
        return Vec3f(this.x - other.x, this.y - other.y, this.z - other.z)
    }

    operator fun plus(other: Vec3f): Vec3f = add(other)
    operator fun minus(other: Vec3f): Vec3f = sub(other)
    operator fun times(scale: Float): Vec3f = Vec3f(this.x * scale, this.y * scale, this.z * scale)

    fun rotatePitch(pitch: Float): Vec3f {
        val f = pitch.fastCos()
        val f1 = pitch.fastSin()

        val d0 = this.x
        val d1 = this.y * f + this.z * f1
        val d2 = this.z * f - this.y * f1

        return Vec3f(d0, d1, d2)
    }

    fun rotateYaw(yaw: Float): Vec3f {
        val f = yaw.fastCos()
        val f1 = yaw.fastSin()

        val d0 = this.x * f + this.z * f1
        val d1 = this.y
        val d2 = this.z * f - this.x * f1

        return Vec3f(d0, d1, d2)
    }

    fun toVec3d() = Vec3d(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())

    companion object {
        @JvmField
        val ZERO = Vec3f(0f, 0f, 0f)
    }
}
