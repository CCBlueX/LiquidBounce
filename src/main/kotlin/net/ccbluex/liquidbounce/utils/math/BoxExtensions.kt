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
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Position
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min

// Box operators

inline operator fun AABB.plus(offset: Position): AABB =
    this.move(offset.x(), offset.y(), offset.z())

inline operator fun AABB.minus(offset: Position): AABB =
    this.move(-offset.x(), -offset.y(), -offset.z())

inline operator fun AABB.plus(offset: Vec3i): AABB =
    this.move(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())

inline operator fun AABB.minus(offset: Vec3i): AABB =
    this.move(-offset.x.toDouble(), -offset.y.toDouble(), -offset.z.toDouble())

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
    val d = p.subtract(start)

    var tEntry = Double.NEGATIVE_INFINITY
    var tExit = Double.POSITIVE_INFINITY

    fun checkSide(axis: Direction.Axis): Boolean {
        val d1 = axis.choose(d.x, d.y, d.z)
        val min = min(axis)
        val max = max(axis)
        val p0 = axis.choose(start.x, start.y, start.z)

        // parallel and outside, no need to check anything else
        if (d1 == 0.0) {
            if (p0 < min || p0 > max) {
                return true
            }
            return false
        }

        val t1 = (min - p0) / d1
        val t2 = (max - p0) / d1
        tEntry = maxOf(tEntry, min(t1, t2))
        tExit = minOf(tExit, max(t1, t2))

        return tEntry > tExit
    }

    if (checkSide(Direction.Axis.X) ||
        checkSide(Direction.Axis.Y) ||
        checkSide(Direction.Axis.Z)
    ) {
        return false
    }

    return tEntry <= tExit
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

    return Vec3(
        minX + spot.x * xsize,
        minY + spot.y * ysize,
        minZ + spot.z * zsize,
    )
}

private fun AABB.pointOnSide(x: Double, y: Double, z: Double, side: Direction): Vec3 =
    when (side) {
        Direction.DOWN -> Vec3(x, minY, z)
        Direction.UP -> Vec3(x, maxY, z)
        Direction.NORTH -> Vec3(x, y, minZ)
        Direction.SOUTH -> Vec3(x, y, maxZ)
        Direction.WEST -> Vec3(minX, y, z)
        Direction.EAST -> Vec3(maxX, y, z)
    }
