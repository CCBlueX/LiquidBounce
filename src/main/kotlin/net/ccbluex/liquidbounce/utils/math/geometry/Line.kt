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
}
