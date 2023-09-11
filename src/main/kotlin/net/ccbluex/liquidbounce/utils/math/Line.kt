package net.ccbluex.liquidbounce.utils.math

import net.minecraft.util.math.Vec3d

data class Line(val position: Vec3d, val direction: Vec3d) {
    companion object {
        fun fromPoints(a: Vec3d, b: Vec3d): Line {
            return Line(a, b - a)
        }
    }

    fun getNearestPointTo(point: Vec3d): Vec3d {
        val plane = Plane(point, direction)

        // If there is no intersection between the created plane and this line it means that the point is in the line.
        return plane.intersection(this) ?: point
    }

    fun squaredDistanceTo(point: Vec3d): Double {
        return this.getNearestPointTo(point).squaredDistanceTo(point)
    }

    fun getPosition(phi: Double): Vec3d {
        return this.position + direction.multiply(phi)
    }
}
