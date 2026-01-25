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

import net.ccbluex.liquidbounce.utils.math.isLikelyZero
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A face. Axis aligned
 */
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

    val area: Double
        get() {
            val dims = dimensions

            return (dims.x * dims.y + dims.y * dims.z + dims.x * dims.z) * 2.0
        }

    val center: Vec3
        get() = Vec3(
            (to.x + from.x) * 0.5,
            (to.y + from.y) * 0.5,
            (to.z + from.z) * 0.5
        )
    val dimensions: Vec3
        get() = Vec3(
            to.x - from.x,
            to.y - from.y,
            to.z - from.z,
        )

    /**
     * If this face is empty, return null, otherwise return this face
     */
    fun requireNonEmpty(): AlignedFace? {
        if (Mth.equal(this.area, 0.0)) {
            return null
        }

        return this
    }

    fun truncateY(minY: Double): AlignedFace {
        val newFace = AlignedFace(
            Vec3(this.from.x, this.from.y.coerceAtLeast(minY), this.from.z),
            Vec3(this.to.x, this.to.y.coerceAtLeast(minY), this.to.z)
        )

        return newFace
    }

    fun clamp(box: AABB): AlignedFace {
        val xRange = box.minX..box.maxX
        val yRange = box.minY..box.maxY
        val zRange = box.minZ..box.maxZ

        val newFrom = Vec3(
            this.from.x.coerceIn(xRange),
            this.from.y.coerceIn(yRange),
            this.from.z.coerceIn(zRange)
        )
        val newTo = Vec3(
            this.to.x.coerceIn(xRange),
            this.to.y.coerceIn(yRange),
            this.to.z.coerceIn(zRange)
        )

        return AlignedFace(newFrom, newTo)
    }

    fun offset(vec: Vec3): AlignedFace {
        return AlignedFace(this.from + vec, this.to + vec)
    }

    fun randomPointOnFace(): Vec3 {
        return Vec3(
            if (from.x == to.x) from.x else Random.nextDouble(from.x, to.x),
            if (from.y == to.y) from.y else Random.nextDouble(from.y, to.y),
            if (from.z == to.z) from.z else Random.nextDouble(from.z, to.z),
        )
    }

    fun coerceInFace(line: Line): LineSegment? {
        val edges = getEdges()

        val nearestPointsToEdges = edges.mapNotNull {
            val (nearestPointOnLine, nearestPointOnFace) = line.getNearestPointsTo(it) ?: return@mapNotNull null

            nearestPointOnFace.distanceToSqr(nearestPointOnLine) to nearestPointOnFace
        }.sortedBy { it.first }

        // If less than 2 points found, we can't form a valid segment
        if (nearestPointsToEdges.size < 2) {
            return null
        }

        val p1 = nearestPointsToEdges[0].second
        val p2 = nearestPointsToEdges[1].second
        val direction = p2.subtract(p1)

        // If points are too close, it's a zero-length segment which is invalid
        if (direction.isLikelyZero) {
            return null
        }

        return LineSegment(p1, direction, 0.0..1.0)
    }

    fun toPlane(): NormalizedPlane {
        val dims = this.dimensions

        val xy = Vec3(
            dims.x,
            dims.y,
            0.0
        )

        val zy = Vec3(
            0.0,
            dims.y,
            dims.z
        )

        return NormalizedPlane.fromParams(this.from, xy, zy)
    }

    /**
     * The face needs to be axis-aligned.
     */
    fun nearestPointTo(otherLine: Line): Vec3 {
        val (d1, d2) = getDirectionVectors()

        val plane = NormalizedPlane.fromParams(this.from, d1, d2)

        val edges = getEdges()

        val intersection = plane.intersection(otherLine)

        if (intersection != null) {
            val isIntersectionInFace = edges.all {
                val lineCenter = it.getPosition(0.5)
                val lineCenterToFaceCenter = lineCenter.subtract(this.center)
                val lineCenterToIntersection = lineCenter.subtract(intersection)

                // Check if the two vectors are pointing in the same direction
                return@all lineCenterToIntersection.dot(lineCenterToFaceCenter) > 0.0
            }

            // Is the intersection in the face?
            // If edges are empty (tiny face), we assume intersection on plane IS in face if it exists
            if (edges.isEmpty() || isIntersectionInFace) {
                return intersection
            }
        }

        val minDistanceToBorder = edges.mapNotNull {
            val (p1, p2) = it.getNearestPointsTo(otherLine) ?: return@mapNotNull null

            p1.distanceToSqr(p2) to p1
        }.minByOrNull { it.first }

        return minDistanceToBorder?.second ?: intersection ?: center
    }

    private fun getEdges(): List<LineSegment> = buildList(4) {
        val (d1, d2) = getDirectionVectors()
        val phiRange = 0.0..1.0

        if (!d1.isLikelyZero) {
            this += LineSegment(from, d1, phiRange)
            this += LineSegment(to, d1.reverse(), phiRange)
        }
        if (!d2.isLikelyZero) {
            this += LineSegment(from, d2, phiRange)
            this += LineSegment(to, d2.reverse(), phiRange)
        }
    }

    private fun getDirectionVectors(): Pair<Vec3, Vec3> {
        val dims = this.dimensions

        // This is a quick hack. If a non-axis-aligned face should be processed, this part just
        // has to be swapped with more robust code.
        return when {
            Mth.equal(dims.x, 0.0) -> {
                Vec3(0.0, dims.y, 0.0) to Vec3(0.0, 0.0, dims.z)
            }

            Mth.equal(dims.y, 0.0) -> {
                Vec3(dims.x, 0.0, 0.0) to Vec3(0.0, 0.0, dims.z)
            }

            Mth.equal(dims.z, 0.0) -> {
                Vec3(0.0, dims.y, 0.0) to Vec3(dims.x, 0.0, 0.0)
            }

            else -> error("Face must be axis aligned for this function to work. dimensions=$dimensions")
        }
    }

}
