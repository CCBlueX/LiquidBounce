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

import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.minecraft.core.Direction
import net.minecraft.core.Position
import net.minecraft.core.Vec3i
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

val AABB.vertices: Array<Vec3>
    get() = arrayOf(
        Vec3(minX, minY, minZ),
        Vec3(minX, minY, maxZ),
        Vec3(minX, maxY, minZ),
        Vec3(minX, maxY, maxZ),
        Vec3(maxX, minY, minZ),
        Vec3(maxX, minY, maxZ),
        Vec3(maxX, maxY, minZ),
        Vec3(maxX, maxY, maxZ),
    )

/**
 * Volume of this box. [AABB] orders its bounds on construction, so the box [AABB.intersect] builds from two disjoint
 * boxes is the gap between them and has a positive volume; use [overlapVolume] for how much two boxes share.
 */
val AABB.volume: Double get() = xsize * ysize * zsize

/**
 * Volume this box and [other] share, zero when they do not intersect.
 */
fun AABB.overlapVolume(other: AABB): Double =
    if (intersects(other)) intersect(other).volume else 0.0

// Box operators

inline operator fun AABB.plus(offset: Position): AABB =
    this.move(offset.x(), offset.y(), offset.z())

inline operator fun AABB.minus(offset: Position): AABB =
    this.move(-offset.x(), -offset.y(), -offset.z())

inline operator fun AABB.plus(offset: Vec3i): AABB =
    this.move(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())

inline operator fun AABB.minus(offset: Vec3i): AABB =
    this.move(-offset.x.toDouble(), -offset.y.toDouble(), -offset.z.toDouble())

data class WorldLocalBox(val origin: Vec3, val localBox: AABB)

fun AABB.worldToLocal(): WorldLocalBox {
    val origin = this.minPosition
    return WorldLocalBox(origin, this - origin)
}

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

/**
 * Get visible sides from [eyes] **outside** the box.
 * @return size in [0..3], 0=inside
 */
fun AABB.visibleSidesTo(eyes: Vec3): List<Direction> = buildList(3) {
    if (eyes.x < minX) {
        this.add(Direction.WEST)
    } else if (eyes.x > maxX) {
        this.add(Direction.EAST)
    }

    if (eyes.y < minY) {
        this.add(Direction.DOWN)
    } else if (eyes.y > maxY) {
        this.add(Direction.UP)
    }

    if (eyes.z < minZ) {
        this.add(Direction.NORTH)
    } else if (eyes.z > maxZ) {
        this.add(Direction.SOUTH)
    }
}

fun AABB.isSideVisible(direction: Direction, eyes: Vec3): Boolean {
    return when (direction) {
        Direction.WEST -> eyes.x < this.minX
        Direction.EAST -> eyes.x > this.maxX
        Direction.DOWN -> eyes.y < this.minY
        Direction.UP -> eyes.y > this.maxY
        Direction.NORTH -> eyes.z < this.minZ
        Direction.SOUTH -> eyes.z > this.maxZ
    }
}


/**
 * Cosine of the smallest angle between [view] (a unit vector) and the direction from [eye]
 * towards any point of [box].
 *
 * On a miss the optimum always lies on a silhouette edge: a stationary point inside a face
 * requires the direction to be parallel to [view], which is either the hit the slab test
 * already caught or the global minimum.
 */
@Suppress("CognitiveComplexMethod")
internal fun maxCosToBox(eye: Vec3, view: Vec3, box: AABB): Double {
    if (rayHitsBox(eye, view, box)) {
        return 1.0
    }

    val xLoFront = eye.x < box.minX
    val xHiFront = eye.x > box.maxX
    val yLoFront = eye.y < box.minY
    val yHiFront = eye.y > box.maxY
    val zLoFront = eye.z < box.minZ
    val zHiFront = eye.z > box.maxZ

    var best = -1.0

    for (yi in 0..1) {
        val y = if (yi == 0) box.minY else box.maxY
        val yFront = if (yi == 0) yLoFront else yHiFront

        for (zi in 0..1) {
            val zFront = if (zi == 0) zLoFront else zHiFront
            if (yFront == zFront) continue

            val z = if (zi == 0) box.minZ else box.maxZ
            best = max(best, edgeMaxCos(eye, view, box.minX, y, z, box.maxX - box.minX, 0.0, 0.0))
        }
    }

    for (xi in 0..1) {
        val x = if (xi == 0) box.minX else box.maxX
        val xFront = if (xi == 0) xLoFront else xHiFront

        for (zi in 0..1) {
            val zFront = if (zi == 0) zLoFront else zHiFront
            if (xFront == zFront) continue

            val z = if (zi == 0) box.minZ else box.maxZ
            best = max(best, edgeMaxCos(eye, view, x, box.minY, z, 0.0, box.maxY - box.minY, 0.0))
        }
    }

    for (xi in 0..1) {
        val x = if (xi == 0) box.minX else box.maxX
        val xFront = if (xi == 0) xLoFront else xHiFront

        for (yi in 0..1) {
            val yFront = if (yi == 0) yLoFront else yHiFront
            if (xFront == yFront) continue

            val y = if (yi == 0) box.minY else box.maxY
            best = max(best, edgeMaxCos(eye, view, x, y, box.minZ, 0.0, 0.0, box.maxZ - box.minZ))
        }
    }

    return best
}

/**
 * Whether the ray `eye + t * view` with `t >= 0` meets [box]. An eye inside the box always hits.
 *
 * `box.clip(eye, eye.fma(t, view)).isPresent`
 *
 * @see AABB.clip
 */
@Suppress("ReturnCount")
private fun rayHitsBox(eye: Vec3, view: Vec3, box: AABB): Boolean {
    var tMin = 0.0
    var tMax = Double.POSITIVE_INFINITY

    if (view.x != 0.0) {
        val t1 = (box.minX - eye.x) / view.x
        val t2 = (box.maxX - eye.x) / view.x
        tMin = max(tMin, min(t1, t2))
        tMax = min(tMax, max(t1, t2))
        if (tMin > tMax) return false
    } else if (eye.x < box.minX || eye.x > box.maxX) {
        return false
    }

    if (view.y != 0.0) {
        val t1 = (box.minY - eye.y) / view.y
        val t2 = (box.maxY - eye.y) / view.y
        tMin = max(tMin, min(t1, t2))
        tMax = min(tMax, max(t1, t2))
        if (tMin > tMax) return false
    } else if (eye.y < box.minY || eye.y > box.maxY) {
        return false
    }

    if (view.z != 0.0) {
        val t1 = (box.minZ - eye.z) / view.z
        val t2 = (box.maxZ - eye.z) / view.z
        tMin = max(tMin, min(t1, t2))
        tMax = min(tMax, max(t1, t2))
        if (tMin > tMax) return false
    } else if (eye.z < box.minZ || eye.z > box.maxZ) {
        return false
    }

    return true
}

/**
 * Largest cosine between [view] and the direction from [eye] towards a point of the edge
 * `(ax, ay, az) + t * (wx, wy, wz)`, `t` in `[0, 1]`. The quadratic term cancels in the
 * derivative, so the endpoints and that single stationary point cover the maximum.
 */
private fun edgeMaxCos(
    eye: Vec3,
    view: Vec3,
    ax: Double,
    ay: Double,
    az: Double,
    wx: Double,
    wy: Double,
    wz: Double,
): Double {
    val px = ax - eye.x
    val py = ay - eye.y
    val pz = az - eye.z

    val a = px * view.x + py * view.y + pz * view.z
    val b = wx * view.x + wy * view.y + wz * view.z
    val c = px * px + py * py + pz * pz
    val d = px * wx + py * wy + pz * wz
    val e = wx * wx + wy * wy + wz * wz

    var best = max(cosAt(a, c), cosAt(a + b, c + 2.0 * d + e))

    // A zero denominator yields NaN or an infinity, which the range check rejects
    val t = (a * d - b * c) / (b * d - a * e)
    if (t > 0.0 && t < 1.0) {
        best = max(best, cosAt(a + b * t, c + 2.0 * d * t + e * t * t))
    }

    return best
}

/**
 * @param along projection of the offset on the view direction
 * @param lengthSquared squared length of that offset
 */
private fun cosAt(along: Double, lengthSquared: Double): Double =
    if (lengthSquared <= 0.0) 1.0 else along / sqrt(lengthSquared)
