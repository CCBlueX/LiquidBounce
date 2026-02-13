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

import it.unimi.dsi.fastutil.doubles.DoubleDoublePair
import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.liquidbounce.utils.math.getCoordinate
import net.ccbluex.liquidbounce.utils.math.getNearestPoint
import net.ccbluex.liquidbounce.utils.math.isLikelyZero
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.preferOver
import net.ccbluex.liquidbounce.utils.math.withLength
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

@Suppress("TooManyFunctions")
open class Line(val position: Vec3, val direction: Vec3) {

    init {
        require(!direction.isLikelyZero) {
            "Direction should be not zero, actual: $direction"
        }
    }

    companion object {
        @JvmStatic
        fun fromPoints(begin: Vec3, end: Vec3): Line {
            return Line(begin, end - begin)
        }
    }

    open fun getNearestPointTo(point: Vec3): Vec3 {
        val plane = NormalizedPlane(point, direction)

        // If there is no intersection between the created plane and this line it means that the point is in the line.
        return plane.intersection(this) ?: point
    }

    fun distanceToSqr(point: Vec3): Double {
        return this.getNearestPointTo(point).distanceToSqr(point)
    }

    fun distanceToSqr(box: AABB): Double {
        val pointOnLine = getNearestPointTo(box)
        val pointOnBox = box.getNearestPoint(pointOnLine)

        return pointOnLine.distanceToSqr(pointOnBox)
    }

    open fun getPositionChecked(phi: Double): Vec3? {
        return this.getPosition(phi)
    }

    open fun getPosition(phi: Double): Vec3 {
        return this.position + direction.scale(phi)
    }

    fun getPhiForPoint(point: Vec3): Double {
        val fromPosition = point.subtract(position)

        val possibleCoordinates = arrayOf(
            doubleArrayOf(fromPosition.x, direction.x),
            doubleArrayOf(fromPosition.y, direction.y),
            doubleArrayOf(fromPosition.z, direction.z)
        ).filter { !Mth.equal(it[1], 0.0) }

        val directionAvg = possibleCoordinates.sumOf { it[1] } / possibleCoordinates.size
        val minAvgDistPair = possibleCoordinates.minByOrNull { abs(it[1] - directionAvg) }!!

        return minAvgDistPair[0] / minAvgDistPair[1]
    }

    /**
     * Returns a tuple with (a) the nearest point of this line to the other line (b) the nearest point of the other
     * line to this line.
     */
    fun getNearestPointsTo(other: Line): Pair<Vec3, Vec3>? {
        val (phi1, phi2) = getNearestPhisTo(other) ?: return null

        return Pair(this.getPosition(phi1), other.getPosition(phi2))
    }

    /**
     * Returns the point on this line that minimizes squared distance to the given axis-aligned box.
     */
    @Suppress("CognitiveComplexMethod", "LongMethod")
    fun getNearestPointTo(box: AABB): Vec3 {
        val px = position.x
        val py = position.y
        val pz = position.z

        val dx = direction.x
        val dy = direction.y
        val dz = direction.z

        val minX = box.minX
        val minY = box.minY
        val minZ = box.minZ
        val maxX = box.maxX
        val maxY = box.maxY
        val maxZ = box.maxZ

        fun distanceSq(phi: Double): Double {
            val x = px + dx * phi
            val y = py + dy * phi
            val z = pz + dz * phi

            val xDiff = when {
                x < minX -> minX - x
                x > maxX -> x - maxX
                else -> 0.0
            }
            val yDiff = when {
                y < minY -> minY - y
                y > maxY -> y - maxY
                else -> 0.0
            }
            val zDiff = when {
                z < minZ -> minZ - z
                z > maxZ -> z - maxZ
                else -> 0.0
            }

            return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff
        }

        val breakpoints = DoubleArray(6)
        var breakpointCount = 0

        fun addBreakpoint(value: Double) {
            breakpoints[breakpointCount++] = value
        }

        if (dx != 0.0) {
            addBreakpoint((minX - px) / dx)
            addBreakpoint((maxX - px) / dx)
        }
        if (dy != 0.0) {
            addBreakpoint((minY - py) / dy)
            addBreakpoint((maxY - py) / dy)
        }
        if (dz != 0.0) {
            addBreakpoint((minZ - pz) / dz)
            addBreakpoint((maxZ - pz) / dz)
        }

        for (i in 1 until breakpointCount) {
            val current = breakpoints[i]
            var j = i - 1
            while (j >= 0 && breakpoints[j] > current) {
                breakpoints[j + 1] = breakpoints[j]
                j--
            }
            breakpoints[j + 1] = current
        }

        var bestPhi = 0.0
        var bestPoint = getPositionChecked(0.0)
        var bestDistance = bestPoint?.let { distanceSq(0.0) } ?: Double.POSITIVE_INFINITY

        fun evaluate(phi: Double) {
            val checked = getPositionChecked(phi) ?: return
            val dist = distanceSq(phi)
            if (dist < bestDistance) {
                bestDistance = dist
                bestPhi = phi
                bestPoint = checked
            }
        }

        for (i in 0 until breakpointCount) {
            evaluate(breakpoints[i])
        }

        fun evaluateInterval(start: Double, end: Double, sample: Double) {
            val sx = px + dx * sample
            val sy = py + dy * sample
            val sz = pz + dz * sample

            var quadraticA = 0.0
            var quadraticB = 0.0

            fun addAxis(sampleCoord: Double, min: Double, max: Double, dir: Double, pos: Double) {
                if (sampleCoord < min) {
                    quadraticA += dir * dir
                    quadraticB += dir * (pos - min)
                } else if (sampleCoord > max) {
                    quadraticA += dir * dir
                    quadraticB += dir * (pos - max)
                }
            }

            addAxis(sx, minX, maxX, dx, px)
            addAxis(sy, minY, maxY, dy, py)
            addAxis(sz, minZ, maxZ, dz, pz)

            if (quadraticA == 0.0) {
                evaluate(sample)
                return
            }

            val root = -quadraticB / quadraticA
            val inInterval = when {
                start == Double.NEGATIVE_INFINITY -> root <= end
                end == Double.POSITIVE_INFINITY -> root >= start
                else -> root in start..end
            }

            if (inInterval) {
                evaluate(root)
            }
        }

        if (breakpointCount == 0) {
            return bestPoint ?: if (this is LineSegment) {
                val startPoint = getPosition(phiRange.start)
                val endPoint = getPosition(phiRange.endInclusive)
                if (distanceSq(phiRange.start) <= distanceSq(phiRange.endInclusive)) startPoint else endPoint
            } else {
                getPosition(0.0)
            }
        }

        evaluateInterval(
            Double.NEGATIVE_INFINITY,
            breakpoints[0],
            breakpoints[0] - 1.0
        )

        for (i in 0 until breakpointCount - 1) {
            val start = breakpoints[i]
            val end = breakpoints[i + 1]
            if (start < end) {
                evaluateInterval(start, end, (start + end) * 0.5)
            }
        }

        evaluateInterval(
            breakpoints[breakpointCount - 1],
            Double.POSITIVE_INFINITY,
            breakpoints[breakpointCount - 1] + 1.0
        )

        return bestPoint ?: if (this is LineSegment) {
            val startPoint = getPosition(phiRange.start)
            val endPoint = getPosition(phiRange.endInclusive)
            if (distanceSq(phiRange.start) <= distanceSq(phiRange.endInclusive)) startPoint else endPoint
        } else {
            getPosition(bestPhi)
        }
    }

    private fun getNearestPhisTo(other: Line): DoubleDoublePair? {
        val phi1 = this.calculateNearestPhiTo(other) ?: return null
        val phi2 = other.calculateNearestPhiTo(this) ?: return null

        return DoubleDoublePair.of(phi1, phi2)
    }

    /**
     * Finds the closest point on the box's surface to the [position] in positive [direction].
     */
    fun getPointOnBoxInDirection(box: AABB): Vec3? {
        return Direction.entries.mapNotNull { dir ->
            val positionCoordinate = position.get(dir.axis)
            val directionCoordinate = direction.get(dir.axis)
            computeIntersection(box.getCoordinate(dir), positionCoordinate, directionCoordinate)?.let { factor ->
                val pointOnFace = dir.unitVec3.scale(factor)
                val directionalPointsOnFace = position.add(direction.withLength(factor))
                pointOnFace.preferOver(directionalPointsOnFace)
            }
        }.minByOrNull(position::distanceToSqr)
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

        if (Mth.equal(divisor, 0.0)) {
            return null
        }

        val t2 =
            -(((dir1Y * dir1Y + dir1X * dir1X) * dir2Z - dir1Y * dir1Z * dir2Y - dir1X * dir1Z * dir2X) * pos2Z + (-dir1Y * dir1Z * dir2Z + (dir1Z * dir1Z + dir1X * dir1X) * dir2Y - dir1X * dir1Y * dir2X) * pos2Y + (-dir1X * dir1Z * dir2Z - dir1X * dir1Y * dir2Y + (dir1Z * dir1Z + dir1Y * dir1Y) * dir2X) * pos2X + ((-dir1Y * dir1Y - dir1X * dir1X) * dir2Z + dir1Y * dir1Z * dir2Y + dir1X * dir1Z * dir2X) * pos1Z + (dir1Y * dir1Z * dir2Z + (-dir1Z * dir1Z - dir1X * dir1X) * dir2Y + dir1X * dir1Y * dir2X) * pos1Y + (dir1X * dir1Z * dir2Z + dir1X * dir1Y * dir2Y + (-dir1Z * dir1Z - dir1Y * dir1Y) * dir2X) * pos1X) / divisor

        return t2
    }

    override fun toString() = "Line(position=$position, direction=$direction)"

}
