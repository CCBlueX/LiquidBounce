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

import net.ccbluex.liquidbounce.utils.math.getCoordinate
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.preferOver
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.abs

@Suppress("TooManyFunctions")
open class Line(val position: Vec3d, val direction: Vec3d) {

    companion object {
        fun fromPoints(p1: Vec3d, p2: Vec3d, normalized: Boolean = false): Line {
            val direction = p2.subtract(p1)
            val finalDirection = if (normalized) direction.normalize() else direction

            return Line(p1, finalDirection)
        }
    }

    open fun getNearestPointTo(point: Vec3d): Vec3d {
        val plane = NormalizedPlane(point, direction)

        // If there is no intersection between the created plane and this line it means that the point is in the line.
        return plane.intersection(this) ?: point
    }

    fun squaredDistanceTo(point: Vec3d): Double {
        return this.getNearestPointTo(point).squaredDistanceTo(point)
    }

    open fun getPositionChcked(phi: Double): Vec3d? {
        return this.position + direction.multiply(phi)
    }

    open fun getPosition(phi: Double): Vec3d {
        return this.position + direction.multiply(phi)
    }

    fun getPhiForPoint(point: Vec3d): Double {
        val fromPosition = point.subtract(position)

        val possibleCoordinates = arrayOf(
            doubleArrayOf(fromPosition.x, direction.x),
            doubleArrayOf(fromPosition.y, direction.y),
            doubleArrayOf(fromPosition.z, direction.z)
        ).filter { !MathHelper.approximatelyEquals(it[1], 0.0) }

        val directionAvg = possibleCoordinates.sumOf { it[1] } / possibleCoordinates.size
        val minAvgDistPair = possibleCoordinates.minByOrNull { abs(it[1] - directionAvg) }!!

        return minAvgDistPair[0] / minAvgDistPair[1]
    }

    /**
     * Returns a tuple with (a) the nearest point of this line to the other line (b) the nearest point of the other
     * line to this line.
     */
    fun getNearestPointsTo(other: Line): Pair<Vec3d, Vec3d>? {
        val (phi1, phi2) = getNearestPhisTo(other) ?: return null

        return Pair(this.getPosition(phi1), other.getPosition(phi2))
    }

    private fun getNearestPhisTo(other: Line): DoubleArray? {
        val phi1 = this.calculateNearestPhiTo(other) ?: return null
        val phi2 = other.calculateNearestPhiTo(this) ?: return null

        return doubleArrayOf(phi1, phi2)
    }

    /**
     * Finds the closest point on the box's surface to the [position] in positive [direction].
     */
    fun getPointOnBoxInDirection(box: Box): Vec3d? {
        val candidates = Direction.entries.mapNotNull { dir ->
            val positionCoordinate = position.getComponentAlongAxis(dir.axis)
            val directionCoordinate = direction.getComponentAlongAxis(dir.axis)
            computeIntersection(box.getCoordinate(dir), positionCoordinate, directionCoordinate)?.let { factor ->
                val pointOnFace = dir.doubleVector.multiply(factor)
                val directionalPointsOnFace = position.add(direction.normalize().multiply(factor))
                pointOnFace.preferOver(directionalPointsOnFace)
            }
        }

        var minDistanceSq = Double.POSITIVE_INFINITY
        var intersection: Vec3d? = null
        candidates.forEach { candidate ->
            if (position.squaredDistanceTo(candidate) < minDistanceSq) {
                minDistanceSq = position.squaredDistanceTo(candidate)
                intersection = candidate
            }
        }

        return intersection
    }

    private fun computeIntersection(plane: Double, pos: Double, dir: Double): Double? {
        if (dir == 0.0) {
            return null
        }

        val t = (plane - pos) / dir
        return if (t > 0) t else null
    }

    @Suppress("MaxLineLength")
    protected open fun calculateNearestPhiTo(other: Line): Double? {
        val pos1X = other.position.x
        val pos1Y = other.position.y
        val pos1Z = other.position.z

        val dir1X = other.direction.x
        val dir1Y = other.direction.y
        val dir1Z = other.direction.z

        val pos2X = this.position.x
        val pos2Y = this.position.y
        val pos2Z = this.position.z

        val dir2X = this.direction.x
        val dir2Y = this.direction.y
        val dir2Z = this.direction.z

        val divisor =
            (dir1Y * dir1Y + dir1X * dir1X) * dir2Z * dir2Z + (-2 * dir1Y * dir1Z * dir2Y - 2 * dir1X * dir1Z * dir2X) * dir2Z + (dir1Z * dir1Z + dir1X * dir1X) * dir2Y * dir2Y - 2 * dir1X * dir1Y * dir2X * dir2Y + (dir1Z * dir1Z + dir1Y * dir1Y) * dir2X * dir2X

        if (MathHelper.approximatelyEquals(divisor, 0.0)) {
            return null
        }

        val t2 =
            -(((dir1Y * dir1Y + dir1X * dir1X) * dir2Z - dir1Y * dir1Z * dir2Y - dir1X * dir1Z * dir2X) * pos2Z + (-dir1Y * dir1Z * dir2Z + (dir1Z * dir1Z + dir1X * dir1X) * dir2Y - dir1X * dir1Y * dir2X) * pos2Y + (-dir1X * dir1Z * dir2Z - dir1X * dir1Y * dir2Y + (dir1Z * dir1Z + dir1Y * dir1Y) * dir2X) * pos2X + ((-dir1Y * dir1Y - dir1X * dir1X) * dir2Z + dir1Y * dir1Z * dir2Y + dir1X * dir1Z * dir2X) * pos1Z + (dir1Y * dir1Z * dir2Z + (-dir1Z * dir1Z - dir1X * dir1X) * dir2Y + dir1X * dir1Y * dir2X) * pos1Y + (dir1X * dir1Z * dir2Z + dir1X * dir1Y * dir2Y + (-dir1Z * dir1Z - dir1Y * dir1Y) * dir2X) * pos1X) / divisor

        return t2
    }

    override fun toString() = "Line(position=$position, direction=$direction)"

}
