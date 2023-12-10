package net.ccbluex.liquidbounce.utils.math.geometry

import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.abs

class LineSegment(position: Vec3d, direction: Vec3d, val phiRange: ClosedFloatingPointRange<Double>): Line(position, direction) {
    val length: Double
        get() = direction.multiply(phiRange.endInclusive - phiRange.start).length()

    val startPoint: Vec3d
        get() = getPosition(phiRange.start)
    val endPoint: Vec3d
        get() = getPosition(phiRange.endInclusive)

    init {
        if (MathHelper.approximatelyEquals(direction.lengthSquared(), 0.0))
            throw IllegalArgumentException("Direction must not be zero")
    }

    override fun getNearestPhiTo(point: Vec3d): Double {
        val plane = NormalizedPlane(point, direction)

        val intersection = plane.intersectionPhi(this)

        // If there is no intersection between the created plane and this line it means that the point is in the line.
        return intersection ?: getPhiForPoint(point)
    }

    override fun getNearestPointTo(point: Vec3d): Vec3d {
        return getPosition(getNearestPhiTo(point))
    }

    override fun getPosition(phi: Double): Vec3d {
        if (phi !in phiRange)
            throw IllegalArgumentException("Phi must be in range $phiRange")

        return super.getPosition(phi)
    }

    companion object {
        fun fromPoints(start: Vec3d, end: Vec3d): LineSegment {
            val direction = end.subtract(start)
            val directionLength = direction.length()

            return LineSegment(start, direction.multiply(1.0 / directionLength), 0.0..directionLength)
        }
    }
}
