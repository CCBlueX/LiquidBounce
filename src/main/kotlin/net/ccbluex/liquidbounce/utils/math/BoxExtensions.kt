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

import net.ccbluex.liquidbounce.utils.client.ceilToInt
import net.ccbluex.liquidbounce.utils.client.floorToInt
import net.ccbluex.liquidbounce.utils.math.geometry.AlignedFace
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Position
import net.minecraft.core.Vec3i
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

// Box operators

inline operator fun AABB.plus(offset: Position): AABB =
    this.move(offset.x(), offset.y(), offset.z())

inline operator fun AABB.minus(offset: Position): AABB =
    this.move(-offset.x(), -offset.y(), -offset.z())

inline operator fun AABB.plus(offset: Vec3i): AABB =
    this.move(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())

inline operator fun AABB.minus(offset: Vec3i): AABB =
    this.move(-offset.x.toDouble(), -offset.y.toDouble(), -offset.z.toDouble())

fun AABB.worldToLocal(): Pair<Vec3, AABB> {
    val origin = this.minPosition
    return origin to (this - origin)
}

fun AABB.iterateBlockPos(
    minYInclusive: Int = minY.floorToInt(),
    maxYInclusive: Int = maxY.ceilToInt(),
): Iterable<BlockPos> =
    BlockPos.betweenClosed(
        minX.floorToInt(),
        minYInclusive,
        minZ.floorToInt(),
        maxX.ceilToInt(),
        maxYInclusive,
        maxZ.ceilToInt(),
    )

fun AABB.iterateBottomLayerBlockPos(): Iterable<BlockPos> =
    iterateBlockPos(maxYInclusive = minY.ceilToInt())

fun AABB.centerOnSide(side: Direction): Vec3 {
    val cx = minX + xsize * 0.5
    val cy = minY + ysize * 0.5
    val cz = minZ + zsize * 0.5

    return pointOnSide(cx, cy, cz, side)
}

/**
 * Tests if the infinite line resulting from [start] and the point [p] will intersect this box.
 */
fun AABB.isHitByLine(start: Vec3, p: Vec3): Boolean {
    return if (start == p) contains(start) else Line.fromPoints(start, p).intersects(this)
}

fun AABB.getCoordinate(direction: Direction): Double =
    if (direction.axisDirection == Direction.AxisDirection.POSITIVE) max(direction.axis) else min(direction.axis)

/** Ray–AABB first hit point (entry or exit). */
fun AABB.firstHit(from: Vec3, to: Vec3): Vec3? =
    if (contains(from)) clip(to, from).orElse(null) else clip(from, to).orElse(null)

/**
 * Get the nearest point of a box. Very useful to calculate the distance of an enemy.
 */
fun AABB.getNearestPoint(from: Position): Vec3 {
    return Vec3(
        from.x().coerceIn(minX, maxX),
        from.y().coerceIn(minY, maxY),
        from.z().coerceIn(minZ, maxZ),
    )
}

/**
 * Squared distance from this box to a point without allocating a temporary [Vec3].
 *
 * @see net.minecraft.world.phys.AABB.distanceToSqr
 */
fun AABB.distanceToSqr(x: Double, y: Double, z: Double): Double {
    val dx = maxOf(minX - x, x - maxX, 0.0)
    val dy = maxOf(minY - y, y - maxY, 0.0)
    val dz = maxOf(minZ - z, z - maxZ, 0.0)
    return Mth.lengthSquared(dx, dy, dz)
}

fun AABB.getNearestPointOnSide(from: Vec3, side: Direction): Vec3 {
    val nearest = getNearestPoint(from)
    return pointOnSide(nearest.x, nearest.y, nearest.z, side)
}

fun AABB.samplePointOnSide(side: Direction, a: Double, b: Double): Vec3 {
    val spot = when (side) {
        Direction.DOWN -> Vec3(a, 0.0, b)
        Direction.UP -> Vec3(a, 1.0, b)
        Direction.NORTH -> Vec3(a, b, 0.0)
        Direction.SOUTH -> Vec3(a, b, 1.0)
        Direction.WEST -> Vec3(0.0, a, b)
        Direction.EAST -> Vec3(1.0, a, b)
    }

    return pointAtProportion(spot.x, spot.y, spot.z)
}

fun AABB.pointAtProportion(p: Double): Vec3 =
    pointAtProportion(p, p, p)

fun AABB.pointAtProportion(pX: Double, pY: Double, pZ: Double): Vec3 = Vec3(
    Math.fma(xsize, pX, minX),
    Math.fma(ysize, pY, minY),
    Math.fma(zsize, pZ, minZ),
)

private fun AABB.pointOnSide(x: Double, y: Double, z: Double, side: Direction): Vec3 =
    when (side) {
        Direction.DOWN -> Vec3(x, minY, z)
        Direction.UP -> Vec3(x, maxY, z)
        Direction.NORTH -> Vec3(x, y, minZ)
        Direction.SOUTH -> Vec3(x, y, maxZ)
        Direction.WEST -> Vec3(minX, y, z)
        Direction.EAST -> Vec3(maxX, y, z)
    }

fun AABB.getFace(direction: Direction): AlignedFace {
    return when (direction) {
        Direction.DOWN -> AlignedFace(
            Vec3(this.minX, this.minY, this.minZ),
            Vec3(this.maxX, this.minY, this.maxZ)
        )

        Direction.UP -> AlignedFace(
            Vec3(this.minX, this.maxY, this.minZ),
            Vec3(this.maxX, this.maxY, this.maxZ)
        )

        Direction.SOUTH -> AlignedFace(
            Vec3(this.minX, this.minY, this.maxZ),
            Vec3(this.maxX, this.maxY, this.maxZ)
        )

        Direction.NORTH -> AlignedFace(
            Vec3(this.minX, this.minY, this.minZ),
            Vec3(this.maxX, this.maxY, this.minZ)
        )

        Direction.EAST -> AlignedFace(
            Vec3(this.maxX, this.minY, this.minZ),
            Vec3(this.maxX, this.maxY, this.maxZ)
        )

        Direction.WEST -> AlignedFace(
            Vec3(this.minX, this.minY, this.minZ),
            Vec3(this.minX, this.maxY, this.maxZ)
        )
    }
}
