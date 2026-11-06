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
package net.ccbluex.liquidbounce.utils.math.geometry

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair
import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.liquidbounce.utils.math.getNearestPoint
import net.ccbluex.liquidbounce.utils.math.isLikelyZero
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.lang.Math.fma
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A face. Axis aligned
 */
@Suppress("TooManyFunctions")
class AlignedFace(from: Vec3, to: Vec3) {
    val from: Vec3 = Vec3(
        min(from.x, to.x),
        min(from.y, to.y),
        min(from.z, to.z),
    )
    val to: Vec3 = Vec3(
        max(from.x, to.x),
        max(from.y, to.y),
        max(from.z, to.z),
    )

    fun asBox(): AABB = AABB(from, to)

    val area: Double
        get() {
            val dims = dimensions
            return dims.x * dims.y + dims.y * dims.z + dims.x * dims.z
        }

    val center: Vec3
        get() = from.lerp(to, 0.5)

    val dimensions: Vec3 = Vec3(
        this.to.x - this.from.x,
        this.to.y - this.from.y,
        this.to.z - this.from.z,
    )

    fun requireNonEmpty(): AlignedFace? =
        takeUnless { Mth.equal(area, 0.0) }

    fun truncateY(minY: Double): AlignedFace =
        AlignedFace(
            Vec3(from.x, from.y.coerceAtLeast(minY), from.z),
            Vec3(to.x, to.y.coerceAtLeast(minY), to.z)
        )

    fun clamp(box: AABB): AlignedFace = AlignedFace(box.getNearestPoint(from), box.getNearestPoint(to))

    fun offset(vec: Vec3): AlignedFace = AlignedFace(from + vec, to + vec)

    fun offset(vec: Vec3i): AlignedFace = AlignedFace(from + vec, to + vec)

    fun randomPointOnFace(): Vec3 {
        return Vec3(
            if (from.x == to.x) from.x else Random.nextDouble(from.x, to.x),
            if (from.y == to.y) from.y else Random.nextDouble(from.y, to.y),
            if (from.z == to.z) from.z else Random.nextDouble(from.z, to.z),
        )
    }

    /**
     * Samples a point on the face by spreading [a] and [b] over its two variable axes.
     *
     * The first variable axis (in x, y, z order) is scaled by [a], the second by [b].
     * Constant (zero width) axes stay pinned to their `from` coordinate.
     */
    fun samplePointOnFace(a: Double, b: Double): Vec3 {
        val dims = dimensions
        return when {
            dims.x == 0.0 -> Vec3(from.x, fma(dims.y, a, from.y), fma(dims.z, b, from.z))
            dims.y == 0.0 -> Vec3(fma(dims.x, a, from.x), from.y, fma(dims.z, b, from.z))
            dims.z == 0.0 -> Vec3(fma(dims.x, a, from.x), fma(dims.y, b, from.y), from.z)
            else -> error("Face must be axis aligned for this function to work. dimensions=$dims")
        }
    }

    /**
     * Walks the sampling grid of this face, spreading [proportions] over its two variable axes.
     *
     * Both axes share [proportions], so the grid always holds `proportions.size ** 2` points regardless of how
     * large the face is.
     *
     * [action] returns `false` to stop the walk, for callers that already found a good enough point. Because
     * this function is inline, a non-local `return` inside [action] also leaves the enclosing function.
     *
     * @return `true` when every point was visited, `false` when [action] stopped the walk
     */
    inline fun forEachSamplePoint(proportions: DoubleArray, action: (Vec3) -> Boolean): Boolean {
        for (a in proportions) {
            for (b in proportions) {
                if (!action(samplePointOnFace(a, b))) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Walks a sampling grid whose points are about [spacing] apart in world space.
     *
     * The point count follows the face size instead of being fixed, so a thin or small face is sampled by
     * fewer points than a large one and each point covers roughly the same area. Points sit at cell centres,
     * so they never land on the face border.
     *
     * The `action` contract and return value are the same as in the proportion overload.
     */
    inline fun forEachSamplePoint(spacing: Double, action: (Vec3) -> Boolean): Boolean {
        require(spacing > 0.0) { "spacing must be positive, was $spacing" }

        val dims = dimensions
        // The two variable axes, in the same order samplePointOnFace spreads a and b over them. A face with
        // two constant axes gives length 0 for the axis it does not use, which collapses that row to a single
        // point instead of duplicating the whole walk.
        val (lengthA, lengthB) = when {
            dims.x == 0.0 -> DoubleDoubleImmutablePair(dims.y, dims.z)
            dims.y == 0.0 -> DoubleDoubleImmutablePair(dims.x, dims.z)
            dims.z == 0.0 -> DoubleDoubleImmutablePair(dims.x, dims.y)
            else -> error("Face must be axis aligned for this function to work. dimensions=$dims")
        }

        return forEachGridCenter(
            max(1, ceil(lengthA / spacing).toInt()),
            max(1, ceil(lengthB / spacing).toInt()),
        ) { a, b -> action(samplePointOnFace(a, b)) }
    }

    fun coerceInFace(line: LinearGeometry3): LineSegment? {
        val nearestPointsToEdges = getEdges().mapNotNull { edge ->
            val (nearestPointOnLine, nearestPointOnFace) = line.getNearestPointsTo(edge) ?: return@mapNotNull null
            nearestPointOnFace.distanceToSqr(nearestPointOnLine) to nearestPointOnFace
        }.sortedBy { it.first }

        if (nearestPointsToEdges.size < 2) {
            return null
        }

        val p1 = nearestPointsToEdges[0].second
        val p2 = nearestPointsToEdges[1].second
        if (p2.subtract(p1).isLikelyZero) {
            return null
        }

        return LineSegment(p1, p2)
    }

    fun toPlane(): NormalizedPlane {
        val (directionA, directionB) = getDirectionVectors()
        return NormalizedPlane.fromParams(from, directionA, directionB)
    }

    fun nearestPointTo(otherLine: LinearGeometry3): Vec3 {
        val (d1, d2) = getDirectionVectors()
        val plane = NormalizedPlane.fromParams(from, d1, d2)
        val edges = getEdges()
        val intersection = plane.intersection(otherLine)
        val faceCenter = center

        if (intersection != null) {
            val isIntersectionInFace = edges.all {
                val lineCenter = it.pointAt(0.5)
                val lineCenterToFaceCenter = lineCenter.subtract(faceCenter)
                val lineCenterToIntersection = lineCenter.subtract(intersection)
                lineCenterToIntersection.dot(lineCenterToFaceCenter) > 0.0
            }

            if (edges.isEmpty() || isIntersectionInFace) {
                return intersection
            }
        }

        val minDistanceToBorder = edges.mapNotNull {
            val (p1, p2) = it.getNearestPointsTo(otherLine) ?: return@mapNotNull null
            p1.distanceToSqr(p2) to p1
        }.minByOrNull { it.first }

        return minDistanceToBorder?.second ?: intersection ?: faceCenter
    }

    private fun getEdges(): List<LineSegment> = buildList(4) {
        val (d1, d2) = getDirectionVectors()

        if (!d1.isLikelyZero) {
            this += LineSegment(from, from + d1)
            this += LineSegment(to, to - d1)
        }
        if (!d2.isLikelyZero) {
            this += LineSegment(from, from + d2)
            this += LineSegment(to, to - d2)
        }
    }

    /**
     * The two in-plane axes of this face. A non-degenerate aligned face is constant on exactly one axis, so its
     * constant axis is exactly zero — the same exactness the factory and the transformations of this class
     * maintain.
     */
    private fun getDirectionVectors(): Pair<Vec3, Vec3> {
        val dims = dimensions

        return when {
            dims.x == 0.0 -> Vec3(0.0, dims.y, 0.0) to Vec3(0.0, 0.0, dims.z)
            dims.y == 0.0 -> Vec3(dims.x, 0.0, 0.0) to Vec3(0.0, 0.0, dims.z)
            dims.z == 0.0 -> Vec3(0.0, dims.y, 0.0) to Vec3(dims.x, 0.0, 0.0)
            else -> error("Face must be axis aligned for this function to work. dimensions=$dims")
        }
    }

    companion {
        fun get(
            direction: Direction,
            minX: Double,
            minY: Double,
            minZ: Double,
            maxX: Double,
            maxY: Double,
            maxZ: Double,
        ): AlignedFace = when (direction) {
            Direction.DOWN -> AlignedFace(
                Vec3(minX, minY, minZ),
                Vec3(maxX, minY, maxZ)
            )

            Direction.UP -> AlignedFace(
                Vec3(minX, maxY, minZ),
                Vec3(maxX, maxY, maxZ)
            )

            Direction.SOUTH -> AlignedFace(
                Vec3(minX, minY, maxZ),
                Vec3(maxX, maxY, maxZ)
            )

            Direction.NORTH -> AlignedFace(
                Vec3(minX, minY, minZ),
                Vec3(maxX, maxY, minZ)
            )

            Direction.EAST -> AlignedFace(
                Vec3(maxX, minY, minZ),
                Vec3(maxX, maxY, maxZ)
            )

            Direction.WEST -> AlignedFace(
                Vec3(minX, minY, minZ),
                Vec3(minX, maxY, maxZ)
            )
        }

        fun get(direction: Direction, box: AABB) = get(
            direction,
            box.minX, box.minY, box.minZ,
            box.maxX, box.maxY, box.maxZ,
        )
    }
}
