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

    fun getNearestPointsTo(other: Line): Pair<Vec3d, Vec3d> {
        val (phi1, phi2) = getNearestPhisTo(other)

        return Pair(this.getPosition(phi1), other.getPosition(phi2))
    }

    fun getNearestPhisTo(other: Line): Pair<Double, Double> {
        val phi1 = this.calculateNearestPhiTo(other)
        val phi2 = other.calculateNearestPhiTo(this)

        return Pair(phi1, phi2)
    }

    protected open fun calculateNearestPhiTo(other: Line): Double {
        val pos1_x = other.position.x
        val pos1_y = other.position.y
        val pos1_z = other.position.z

        val dir1_x = other.direction.x
        val dir1_y = other.direction.y
        val dir1_z = other.direction.z

        val pos2_x = this.position.x
        val pos2_y = this.position.y
        val pos2_z = this.position.z

        val dir2_x = this.direction.x
        val dir2_y = this.direction.y
        val dir2_z = this.direction.z

        val t2 =
            -(((dir1_y * dir1_y + dir1_x * dir1_x) * dir2_z - dir1_y * dir1_z * dir2_y - dir1_x * dir1_z * dir2_x) * pos2_z + (-dir1_y * dir1_z * dir2_z + (dir1_z * dir1_z + dir1_x * dir1_x) * dir2_y - dir1_x * dir1_y * dir2_x) * pos2_y + (-dir1_x * dir1_z * dir2_z - dir1_x * dir1_y * dir2_y + (dir1_z * dir1_z + dir1_y * dir1_y) * dir2_x) * pos2_x + ((-dir1_y * dir1_y - dir1_x * dir1_x) * dir2_z + dir1_y * dir1_z * dir2_y + dir1_x * dir1_z * dir2_x) * pos1_z + (dir1_y * dir1_z * dir2_z + (-dir1_z * dir1_z - dir1_x * dir1_x) * dir2_y + dir1_x * dir1_y * dir2_x) * pos1_y + (dir1_x * dir1_z * dir2_z + dir1_x * dir1_y * dir2_y + (-dir1_z * dir1_z - dir1_y * dir1_y) * dir2_x) * pos1_x) / ((dir1_y * dir1_y + dir1_x * dir1_x) * dir2_z * dir2_z + (-2 * dir1_y * dir1_z * dir2_y - 2 * dir1_x * dir1_z * dir2_x) * dir2_z + (dir1_z * dir1_z + dir1_x * dir1_x) * dir2_y * dir2_y - 2 * dir1_x * dir1_y * dir2_x * dir2_y + (dir1_z * dir1_z + dir1_y * dir1_y) * dir2_x * dir2_x)
        return t2
    }
}
