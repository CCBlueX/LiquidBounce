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
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.core.Vec3i
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
            return dims.x * dims.y + dims.y * dims.z + dims.x * dims.z
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

    fun requireNonEmpty(): AlignedFace? {
        if (Mth.equal(area, 0.0)) {
            return null
        }

        return this
    }

    fun truncateY(minY: Double): AlignedFace {
        return AlignedFace(
            Vec3(from.x, from.y.coerceAtLeast(minY), from.z),
            Vec3(to.x, to.y.coerceAtLeast(minY), to.z)
        )
    }

    fun clamp(box: AABB): AlignedFace {
        val xRange = box.minX..box.maxX
        val yRange = box.minY..box.maxY
        val zRange = box.minZ..box.maxZ

        return AlignedFace(
            Vec3(
                from.x.coerceIn(xRange),
                from.y.coerceIn(yRange),
                from.z.coerceIn(zRange)
            ),
            Vec3(
                to.x.coerceIn(xRange),
                to.y.coerceIn(yRange),
                to.z.coerceIn(zRange)
            )
        )
    }

    fun offset(vec: Vec3): AlignedFace {
        return AlignedFace(from + vec, to + vec)
    }

    fun offset(vec: Vec3i): AlignedFace {
        return AlignedFace(from + vec, to + vec)
    }

    fun randomPointOnFace(): Vec3 {
        return Vec3(
            if (from.x == to.x) from.x else Random.nextDouble(from.x, to.x),
            if (from.y == to.y) from.y else Random.nextDouble(from.y, to.y),
            if (from.z == to.z) from.z else Random.nextDouble(from.z, to.z),
        )
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
        val dims = dimensions

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

        return NormalizedPlane.fromParams(from, xy, zy)
    }

    fun nearestPointTo(otherLine: LinearGeometry3): Vec3 {
        val (d1, d2) = getDirectionVectors()
        val plane = NormalizedPlane.fromParams(from, d1, d2)
        val edges = getEdges()
        val intersection = plane.intersection(otherLine)

        if (intersection != null) {
            val isIntersectionInFace = edges.all {
                val lineCenter = it.pointAt(0.5)
                val lineCenterToFaceCenter = lineCenter.subtract(center)
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

        return minDistanceToBorder?.second ?: intersection ?: center
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

    private fun getDirectionVectors(): Pair<Vec3, Vec3> {
        val dims = dimensions

        return when {
            Mth.equal(dims.x, 0.0) -> Vec3(0.0, dims.y, 0.0) to Vec3(0.0, 0.0, dims.z)
            Mth.equal(dims.y, 0.0) -> Vec3(dims.x, 0.0, 0.0) to Vec3(0.0, 0.0, dims.z)
            Mth.equal(dims.z, 0.0) -> Vec3(0.0, dims.y, 0.0) to Vec3(dims.x, 0.0, 0.0)
            else -> error("Face must be axis aligned for this function to work. dimensions=$dimensions")
        }
    }
}
