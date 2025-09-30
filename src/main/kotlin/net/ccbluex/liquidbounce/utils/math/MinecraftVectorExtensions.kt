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
@file:Suppress("NOTHING_TO_INLINE", "TooManyFunctions")

package net.ccbluex.liquidbounce.utils.math

import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.minecraft.util.math.*
import kotlin.math.floor

inline operator fun BlockPos.rangeTo(other: BlockPos): BlockBox = BlockBox.create(this, other)

inline operator fun Vec3i.component1() = this.x
inline operator fun Vec3i.component2() = this.y
inline operator fun Vec3i.component3() = this.z

inline fun BlockPos.copy(x: Int = this.x, y: Int = this.y, z: Int = this.z) = BlockPos(x, y, z)

inline operator fun Vec3i.plus(other: Vec3i): Vec3i {
    return this.add(other)
}

inline operator fun Vec3i.minus(other: Vec3i): Vec3i {
    return this.subtract(other)
}

inline operator fun Vec3i.times(scalar: Int): Vec3i {
    return this.multiply(scalar)
}

inline operator fun Vec3d.plus(other: Vec3d): Vec3d {
    return this.add(other)
}

inline operator fun Vec3d.minus(other: Vec3d): Vec3d {
    return this.subtract(other)
}

inline operator fun Vec3d.times(scalar: Double): Vec3d {
    return this.multiply(scalar)
}

val Vec3d.isLikelyZero: Boolean
    get() = MathHelper.approximatelyEquals(this.lengthSquared(), 1.0E-6)

inline fun Vec3d.interpolate(start: Vec3d, multiple: Double) =
    Vec3d(
        this.x.interpolate(start.x, multiple),
        this.y.interpolate(start.y, multiple),
        this.z.interpolate(start.z, multiple),
    )

inline fun Double.interpolate(old: Double, scale: Double) = old + (this - old) * scale

inline fun Vec3d.copy(x: Double = this.x, y: Double = this.y, z: Double = this.z) = Vec3d(x, y, z)

inline operator fun Vec3d.component1(): Double = this.x
inline operator fun Vec3d.component2(): Double = this.y
inline operator fun Vec3d.component3(): Double = this.z

fun Iterable<Vec3d>.average(): Vec3d {
    val result = Vec3d(0.0, 0.0, 0.0)
    var i = 0
    for (vec in this) {
        result.move(vec)
        i++
    }
    return result.scale(1.0 / i)
}

inline fun forEach3D(v0: Vec3d, v1: Vec3d, step: Double, fn: (Double, Double, Double) -> Unit) {
    val (startX, startY, startZ) = v0
    val (endX, endY, endZ) = v1

    var x = startX
    while (x <= endX) {
        var y = startY
        while (y <= endY) {
            var z = startZ
            while (z <= endZ) {
                fn(x, y, z)

                z += step
            }
            y += step
        }
        x += step
    }
}

inline fun forEach3D(v0: Vec3i, v1: Vec3i, step: Int = 1, fn: (Int, Int, Int) -> Unit) {
    val (startX, startY, startZ) = v0
    val (endX, endY, endZ) = v1

    var x = startX
    while (x <= endX) {
        var y = startY
        while (y <= endY) {
            var z = startZ
            while (z <= endZ) {
                fn(x, y, z)

                z += step
            }
            y += step
        }
        x += step
    }
}

fun Vec3i.toVec3d(): Vec3d = Vec3d.of(this)
fun Vec3i.toVec3d(
    xOffset: Double = 0.0,
    yOffset: Double = 0.0,
    zOffset: Double = 0.0,
): Vec3d = Vec3d(x + xOffset, y + yOffset, z + zOffset)

fun Vec3d.toVec3() = Vec3(this.x, this.y, this.z)
fun Vec3d.toVec3i() = Vec3i(this.x.toInt(), this.y.toInt(), this.z.toInt())

fun Vec3d.toBlockPos(
    xOffset: Double = 0.0,
    yOffset: Double = 0.0,
    zOffset: Double = 0.0,
): BlockPos = BlockPos.ofFloored(x + xOffset, y + yOffset, z + zOffset)

fun Vec3d.preferOver(other: Vec3d): Vec3d {
    val x = if (this.x == 0.0) other.x else this.x
    val y = if (this.y == 0.0) other.y else this.y
    val z = if (this.z == 0.0) other.z else this.z
    return Vec3d(x, y, z)
}

// Mutable Vec3d

fun Vec3d.set(x: Double = this.x, y: Double = this.y, z: Double = this.z): Vec3d = apply {
    this.x = x
    this.y = y
    this.z = z
}

fun Vec3d.set(other: Vec3d): Vec3d = set(other.x, other.y, other.z)

fun Vec3d.move(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Vec3d = apply {
    this.x += x
    this.y += y
    this.z += z
}

fun Vec3d.move(other: Vec3d): Vec3d = move(other.x, other.y, other.z)

fun Vec3d.scale(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Vec3d = apply {
    this.x *= x
    this.y *= y
    this.z *= z
}

fun Vec3d.scale(scale: Double = 1.0): Vec3d = scale(x = scale, y = scale, z = scale)
