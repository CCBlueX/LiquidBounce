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
package net.ccbluex.liquidbounce.utils.math.geometry

import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A face. Axis aligned
 */
class AlignedFace(from: Vec3d, to: Vec3d) {
    val from: Vec3d = Vec3d(
        min(from.x, to.x),
        min(from.y, to.y),
        min(from.z, to.z),
    )
    val to: Vec3d = Vec3d(
        max(from.x, to.x),
        max(from.y, to.y),
        max(from.z, to.z),
    )

    val area: Double
        get() {
            val dims = dimensions

            return (dims.x * dims.y + dims.y * dims.z + dims.x * dims.z) * 2.0
        }

    val center: Vec3d
        get() = Vec3d(
            (to.x + from.x) * 0.5,
            (to.y + from.y) * 0.5,
            (to.z + from.z) * 0.5
        )
    val dimensions: Vec3d
        get() = Vec3d(
            to.x - from.x,
            to.y - from.y,
            to.z - from.z,
        )

    /**
     * If this face is empty, return null, otherwise return this face
     */
    fun requireNonEmpty(): AlignedFace? {
        if (MathHelper.approximatelyEquals(this.area, 0.0)) {
            return null
        }

        return this
    }

    fun truncateY(minY: Double): AlignedFace {
        val newFace = AlignedFace(
            Vec3d(this.from.x, this.from.y.coerceAtLeast(minY), this.from.z),
            Vec3d(this.to.x, this.to.y.coerceAtLeast(minY), this.to.z)
        )

        return newFace
    }

    fun clamp(box: Box): AlignedFace {
        val xRange = box.minX..box.maxX
        val yRange = box.minY..box.maxY
        val zRange = box.minZ..box.maxZ

        val newFrom = Vec3d(
            this.from.x.coerceIn(xRange),
            this.from.y.coerceIn(yRange),
            this.from.z.coerceIn(zRange)
        )
        val newTo = Vec3d(
            this.to.x.coerceIn(xRange),
            this.to.y.coerceIn(yRange),
            this.to.z.coerceIn(zRange)
        )

        return AlignedFace(newFrom, newTo)
    }

    fun offset(vec: Vec3d): AlignedFace {
        return AlignedFace(this.from.add(vec), this.to.add(vec))
    }


    fun randomPointOnFace(): Vec3d {
        return Vec3d(
            if (from.x == to.x) from.x else Random.nextDouble(from.x, to.x),
            if (from.y == to.y) from.y else Random.nextDouble(from.y, to.y),
            if (from.z == to.z) from.z else Random.nextDouble(from.z, to.z),
        )
    }

    fun coerceInFace(line: Line): LineSegment {
        val edges = getEdges()

        val nearestPointsToEdges = edges.mapNotNull {
            val (nearestPointOnLine, nearestPointOnFace) = line.getNearestPointsTo(it) ?: return@mapNotNull null

            nearestPointOnFace.squaredDistanceTo(nearestPointOnLine) to nearestPointOnFace
        }.sortedBy { it.first }

        return LineSegment.fromPoints(nearestPointsToEdges[0].second, nearestPointsToEdges[1].second)
    }

    fun toPlane(): NormalizedPlane {
        val dims = this.dimensions

        val xy = Vec3d(
            dims.x,
            dims.y,
            0.0
        )

        val zy = Vec3d(
            0.0,
            dims.y,
            dims.z
        )

        return NormalizedPlane.fromParams(this.from, xy, zy)
    }

    /**
     * The face needs to be axis-aligned.
     */
    fun nearestPointTo(otherLine: Line): Vec3d {
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
                return@all lineCenterToIntersection.dotProduct(lineCenterToFaceCenter) > 0.0
            }

            // Is the intersection in the face?
            if (isIntersectionInFace) {
                return intersection
            }
        }

        val minDistanceToBorder = edges.mapNotNull {
            val (p1, p2) = it.getNearestPointsTo(otherLine) ?: return@mapNotNull null

            p1 to p1.squaredDistanceTo(p2)
        }.minBy { it.second }

        return minDistanceToBorder.first
    }

    private fun getEdges(): List<LineSegment> {
        val (d1, d2) = getDirectionVectors()
        val phiRange = 0.0..1.0

        return listOf(
            LineSegment(from, d1, phiRange),
            LineSegment(from, d2, phiRange),
            LineSegment(to, d1.negate(), phiRange),
            LineSegment(to, d2.negate(), phiRange)
        )
    }

    private fun getDirectionVectors(): Pair<Vec3d, Vec3d> {
        val dims = this.dimensions

        // This is a quick hack. If a non-axis-aligned face should be processed, this part just
        // has to be swapped with more robust code.
        return when {
            MathHelper.approximatelyEquals(dims.x, 0.0) -> {
                Vec3d(0.0, dims.y, 0.0) to Vec3d(0.0, 0.0, dims.z)
            }

            MathHelper.approximatelyEquals(dims.y, 0.0) -> {
                Vec3d(dims.x, 0.0, 0.0) to Vec3d(0.0, 0.0, dims.z)
            }

            MathHelper.approximatelyEquals(dims.z, 0.0) -> {
                Vec3d(0.0, dims.y, 0.0) to Vec3d(dims.x, 0.0, 0.0)
            }

            else -> error("Face must be axis aligned for this function to work.")
        }
    }

}
