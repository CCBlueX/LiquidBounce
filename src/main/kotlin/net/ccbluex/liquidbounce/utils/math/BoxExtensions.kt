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

import net.minecraft.core.Direction
import net.minecraft.core.Position
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull
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

fun AABB.centerPointOf(side: Direction): Vec3 {
    return when (side) {
        Direction.DOWN -> Vec3(minX + xsize * 0.5, minY, minZ + zsize * 0.5)
        Direction.UP -> Vec3(minX + xsize * 0.5, maxY, minZ + zsize * 0.5)
        Direction.NORTH -> Vec3(minX + xsize * 0.5, minY + ysize * 0.5, minZ)
        Direction.SOUTH -> Vec3(minX + xsize * 0.5, minY + ysize * 0.5, maxZ)
        Direction.WEST -> Vec3(minX, minY + ysize * 0.5, minZ + zsize * 0.5)
        Direction.EAST -> Vec3(maxX, minY + ysize * 0.5, minZ + zsize * 0.5)
    }
}

/**
 * Tests if the infinite line resulting from [start] and the point [p] will intersect this box.
 */
fun AABB.isHitByLine(start: Vec3, p: Vec3): Boolean {
    val d = p.subtract(start)

    var tEntry = Double.NEGATIVE_INFINITY
    var tExit = Double.POSITIVE_INFINITY

    fun AABB.checkSide(axis: Direction.Axis, start: Vec3, d: Vec3): Boolean {
        val d1 = axis.choose(d.x, d.y, d.z)
        val min = min(axis)
        val max = max(axis)
        val p0 = axis.choose(start.x, start.y, start.z)

        // parallel and outside, no need to check anything else
        if (d1 == 0.0 && (p0 !in min..max)) {
            return true
        }

        val t1 = (min - p0) / d1
        val t2 = (max - p0) / d1
        val tMin = min(t1, t2)
        val tMax = max(t1, t2)

        tEntry = maxOf(tEntry, tMin)
        tExit = minOf(tExit, tMax)

        return tEntry > tExit
    }

    if (checkSide(Direction.Axis.X, start, d) ||
        checkSide(Direction.Axis.Y, start, d) ||
        checkSide(Direction.Axis.Z, start, d)) {
        return false
    }

    return tEntry <= tExit
}

fun AABB.getCoordinate(direction: Direction): Double {
    return if (direction.axisDirection == Direction.AxisDirection.POSITIVE) {
        this.max(direction.axis)
    } else {
        this.min(direction.axis)
    }
}

/** Ray–AABB first hit point (entry or exit). */
fun AABB.firstHit(from: Vec3, to: Vec3): Vec3? {
    return if (contains(from)) {
        clip(to, from).getOrNull()
    } else {
        clip(from, to).getOrNull()
    }
}
