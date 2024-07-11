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

import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.abs

open class Line(val position: Vec3d, val direction: Vec3d) {

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

        val possibleCoordinates = mutableListOf(
            Pair(fromPosition.x, direction.x),
            Pair(fromPosition.y, direction.y),
            Pair(fromPosition.z, direction.z)
        )
            .filter { !MathHelper.approximatelyEquals(it.second, 0.0) }

        val directionAvg = possibleCoordinates.map { it.second }.average()
        val minAvgDistPair = possibleCoordinates.minByOrNull { abs(it.second - directionAvg) }!!

        return minAvgDistPair.first / minAvgDistPair.second
    }

    /**
     * Returns a tuple with (a) the nearest point of this line to the other line (b) the nearest point of the other
     * line to this line.
     */
    fun getNearestPointsTo(other: Line): Pair<Vec3d, Vec3d>? {
        val (phi1, phi2) = getNearestPhisTo(other) ?: return null

        return Pair(this.getPosition(phi1), other.getPosition(phi2))
    }

    fun getNearestPhisTo(other: Line): Pair<Double, Double>? {
        val phi1 = this.calculateNearestPhiTo(other) ?: return null
        val phi2 = other.calculateNearestPhiTo(this) ?: return null

        return Pair(phi1, phi2)
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
}
