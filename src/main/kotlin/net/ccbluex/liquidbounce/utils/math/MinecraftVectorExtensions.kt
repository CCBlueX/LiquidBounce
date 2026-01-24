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
@file:Suppress("NOTHING_TO_INLINE", "TooManyFunctions")

package net.ccbluex.liquidbounce.utils.math

import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.minecraft.core.BlockPos
import net.minecraft.core.Position
import net.minecraft.core.Vec3i
import net.minecraft.util.Mth
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.joml.Vector3fc
import kotlin.math.abs
import kotlin.math.sqrt

inline operator fun Vec2.component1() = this.x
inline operator fun Vec2.component2() = this.y
inline fun Vec2.copy(x: Float = this.x, y: Float = this.y) = Vec2(x, y)

inline operator fun BlockPos.rangeTo(other: BlockPos): BoundingBox = BoundingBox.fromCorners(this, other)

inline fun BlockPos.MutableBlockPos.set(pos: Position): BlockPos.MutableBlockPos = set(pos.x(), pos.y(), pos.z())

inline operator fun Vec3i.component1() = this.x
inline operator fun Vec3i.component2() = this.y
inline operator fun Vec3i.component3() = this.z

inline fun BlockPos.copy(x: Int = this.x, y: Int = this.y, z: Int = this.z) = BlockPos(x, y, z)

inline operator fun Vec3i.plus(other: Vec3i): Vec3i = offset(other)

inline operator fun Vec3i.minus(other: Vec3i): Vec3i = subtract(other)

inline operator fun Vec3i.times(scalar: Int): Vec3i = multiply(scalar)

inline operator fun Vec3.plus(other: Position): Vec3 = add(other.x(), other.y(), other.z())

inline operator fun Vec3.plus(other: Vec3i): Vec3 = add(other.x.toDouble(), other.y.toDouble(), other.z.toDouble())

inline operator fun Vec3.minus(other: Position): Vec3 = subtract(other.x(), other.y(), other.z())

inline operator fun Vec3.minus(other: Vec3i): Vec3 =
    subtract(other.x.toDouble(), other.y.toDouble(), other.z.toDouble())

inline operator fun Vec3.times(scalar: Double): Vec3 = scale(scalar)

/**
 * `this.normalize().scale(newLength)`
 *
 * @return a [Vec3] with same direction as the receiver and length of [newLength].
 */
fun Vec3.withLength(newLength: Double): Vec3 {
    val lengthSq = lengthSqr()
    return if (Mth.equal(lengthSq, 0.0)) Vec3.ZERO else scale(newLength / sqrt(lengthSq))
}

@JvmOverloads
fun Vec3.isNormalized(tolerance: Double = 1e-4): Boolean =
    abs(this.lengthSqr() - 1.0) < tolerance

@JvmOverloads
fun Vec3.normalizeIfNeeded(tolerance: Double = 1e-4): Vec3 =
    if (isNormalized(tolerance)) this else normalize()

inline val Vec3.isLikelyZero: Boolean
    get() = Mth.equal(this.lengthSqr(), 0.0)

inline val Vec2.isLikelyZero: Boolean
    get() = Mth.equal(this.lengthSquared(), 0.0F)

inline fun Vec3.copy(x: Double = this.x, y: Double = this.y, z: Double = this.z) = Vec3(x, y, z)

fun Vector3fc.toVec3d(): Vec3 =
    Vec3(this.x().toDouble(), this.y().toDouble(), this.z().toDouble())

inline fun Vector3f.set(vec3d: Vec3): Vector3f =
    set(vec3d.x, vec3d.y, vec3d.z)

inline fun Vector3f.add(vec3d: Vec3): Vector3f =
    add(vec3d.x.toFloat(), vec3d.y.toFloat(), vec3d.z.toFloat())

inline fun Vector3f.sub(vec3d: Vec3): Vector3f =
    sub(vec3d.x.toFloat(), vec3d.y.toFloat(), vec3d.z.toFloat())

inline fun Vec3.multiply(factorX: Float = 1.0f, factorY: Float = 1.0f, factorZ: Float = 1.0f): Vec3 =
    multiply(factorX.toDouble(), factorY.toDouble(), factorZ.toDouble())

inline fun Vec3.multiply(factorX: Double = 1.0, factorY: Double = 1.0, factorZ: Double = 1.0): Vec3 =
    multiply(factorX, factorY, factorZ)

inline operator fun Vec3.component1(): Double = this.x
inline operator fun Vec3.component2(): Double = this.y
inline operator fun Vec3.component3(): Double = this.z

operator fun ChunkPos.contains(blockPos: Long): Boolean =
    BlockPos.getX(blockPos) in minBlockX..maxBlockX && BlockPos.getZ(blockPos) in minBlockZ..maxBlockZ

fun Iterable<Vec3>.average(): Vec3 {
    val result = Vec3(0.0, 0.0, 0.0)
    var i = 0
    for (vec in this) {
        result.move(vec)
        i++
    }
    return result.scaleMut(1.0 / i)
}

inline fun Vec3i.toVec3d(
    xOffset: Double = 0.0,
    yOffset: Double = 0.0,
    zOffset: Double = 0.0,
): Vec3 = Vec3(x + xOffset, y + yOffset, z + zOffset)

inline fun Vec3.toVec3f(): Vec3f = Vec3f(this.x, this.y, this.z)

@Deprecated("use this.toBlockPos instead", replaceWith = ReplaceWith("this.toBlockPos"))
inline fun Vec3.toVec3i(): Vec3i = toBlockPos()

inline fun Vec3.toBlockPos(
    xOffset: Double = 0.0,
    yOffset: Double = 0.0,
    zOffset: Double = 0.0,
): BlockPos = BlockPos.containing(x + xOffset, y + yOffset, z + zOffset)

fun Vec3.preferOver(other: Vec3): Vec3 {
    val x = if (this.x == 0.0) other.x else this.x
    val y = if (this.y == 0.0) other.y else this.y
    val z = if (this.z == 0.0) other.z else this.z
    return Vec3(x, y, z)
}

// Mutable Vec3d

fun Vec3.set(x: Double = this.x, y: Double = this.y, z: Double = this.z): Vec3 = apply {
    this.x = x
    this.y = y
    this.z = z
}

fun Vec3.set(other: Vec3): Vec3 = set(other.x, other.y, other.z)

fun Vec3.move(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Vec3 = apply {
    this.x += x
    this.y += y
    this.z += z
}

fun Vec3.move(other: Vec3): Vec3 = move(other.x, other.y, other.z)

fun Vec3.scaleMut(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Vec3 = apply {
    this.x *= x
    this.y *= y
    this.z *= z
}

fun Vec3.scaleMut(scale: Double = 1.0): Vec3 = scaleMut(x = scale, y = scale, z = scale)
