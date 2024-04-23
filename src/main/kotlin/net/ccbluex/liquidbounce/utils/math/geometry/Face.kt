/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
class Face(from: Vec3d, to: Vec3d) {
    val from: Vec3d
    val to: Vec3d

    init {
        this.from = Vec3d(
            min(from.x, to.x),
            min(from.y, to.y),
            min(from.z, to.z),
        )
        this.to = Vec3d(
            max(from.x, to.x),
            max(from.y, to.y),
            max(from.z, to.z),
        )
    }

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
    fun requireNonEmpty(): Face? {
        if (MathHelper.approximatelyEquals(this.area, 0.0))
            return null

        return this
    }

    fun intersect(other: NormalizedPlane) {
        val dims = this.dimensions
        val intersectLine = this.toPlane().intersection(other) ?: return

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


        val phiRange = 0.0..1.0
        val lines = listOf(
            LineSegment(this.from, xy, phiRange),
            LineSegment(this.from, zy, phiRange),
            LineSegment(this.to, xy.negate(), phiRange),
            LineSegment(this.to, zy.negate(), phiRange)
        )

//        val intersections = lines.mapNotNull { line ->
//            line.getNearestPointTo()
//        }
    }

    fun truncateY(minY: Double): Face {
        val newFace = Face(
            Vec3d(this.from.x, this.from.y.coerceAtLeast(minY), this.from.z),
            Vec3d(this.to.x, this.to.y.coerceAtLeast(minY), this.to.z)
        )

        return newFace
    }

    fun clamp(box: Box): Face {
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

        return Face(newFrom, newTo)
    }

    fun offset(vec: Vec3d): Face {
        return Face(this.from.add(vec), this.to.add(vec))
    }


    fun randomPointOnFace(): Vec3d {
        return Vec3d(
            if (from.x == to.x) from.x else Random.nextDouble(from.x, to.x),
            if (from.y == to.y) from.y else Random.nextDouble(from.y, to.y),
            if (from.z == to.z) from.z else Random.nextDouble(from.z, to.z),
        )
    }

    fun coerceInFace(point: Vec3d): Vec3d {
        return Vec3d(
            point.x.coerceIn(this.from.x, this.to.x),
            point.y.coerceIn(this.from.y, this.to.z),
            point.z.coerceIn(this.from.z, this.to.z),
        )
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
     * Note that this function is a good approximation but no perfect mathematical solution
     */
    fun nearestPointTo(otherLine: Line): Vec3d? {
        val dims = this.dimensions

        val (d1, d2) = when {
            MathHelper.approximatelyEquals(dims.x, 0.0) -> {
                Vec3d(0.0, dims.y, 0.0) to Vec3d(0.0, 0.0, dims.z)
            }

            MathHelper.approximatelyEquals(dims.y, 0.0) -> {
                Vec3d(dims.x, 0.0, 0.0) to Vec3d(0.0, 0.0, dims.z)
            }

            MathHelper.approximatelyEquals(dims.z, 0.0) -> {
                Vec3d(0.0, dims.y, 0.0) to Vec3d(dims.x, 0.0, 0.0)
            }

            else -> throw IllegalStateException("Nope.")
        }

        val plane = NormalizedPlane.fromParams(this.from, d1, d2)

        val phiRange = 0.0..1.0

        val lines = listOf(
            LineSegment(this.from, d1, phiRange),
            LineSegment(this.from, d2, phiRange),
            LineSegment(this.to, d1.negate(), phiRange),
            LineSegment(this.to, d2.negate(), phiRange)
        )

        val intersection = plane.intersection(otherLine)

        if (intersection != null) {
            val isIntersectionInFace = lines.all {
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

        val minDistanceToBorder = lines.map {
            val (p1, p2) = it.getNearestPointsTo(otherLine)

            p1 to p1.squaredDistanceTo(p2)
        }.minBy { it.second }

        return minDistanceToBorder.first
    }

}
