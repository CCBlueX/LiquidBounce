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
package net.ccbluex.liquidbounce.render.engine.type

import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.minecraft.core.Position
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

@Suppress("TooManyFunctions")
@JvmRecord
data class Vec3f(val x: Float, val y: Float, val z: Float) {
    constructor(x: Double, y: Double, z: Double) : this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(vec: Position) : this(vec.x(), vec.y(), vec.z())
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

    fun rotateX(angle: Float): Vec3f {
        val cos = angle.fastCos()
        val sin = angle.fastSin()

        return Vec3f(
            this.x,
            this.y * cos + this.z * sin,
            this.z * cos - this.y * sin,
        )
    }

    fun rotateY(angle: Float): Vec3f {
        val cos = angle.fastCos()
        val sin = angle.fastSin()

        return Vec3f(
            this.x * cos + this.z * sin,
            this.y,
            this.z * cos - this.x * sin,
        )
    }

    fun lengthSqr(): Float = x * x + y * y + z * z

    fun length(): Float = sqrt(lengthSqr())

    fun normalized(): Vec3f = this * (1f / length())

    operator fun unaryMinus(): Vec3f = Vec3f(-this.x, -this.y, -this.z)

    fun toVec3d() = Vec3(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())

    companion object {
        @JvmField
        val ZERO = Vec3f(0f, 0f, 0f)
    }
}
