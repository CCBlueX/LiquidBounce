package net.ccbluex.liquidbounce.utils.math.geometry

import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.abs

class LineSegment(position: Vec3d, direction: Vec3d, val phiRange: ClosedFloatingPointRange<Double>): Line(position, direction) {
    val length: Double
        get() = direction.multiply(phiRange.endInclusive - phiRange.start).length()

    init {
        if (MathHelper.approximatelyEquals(direction.lengthSquared(), 0.0))
            throw IllegalArgumentException("Direction must not be zero")
    }

    override fun getNearestPointTo(point: Vec3d): Vec3d {
        val plane = NormalizedPlane(point, direction)

        // If there is no intersection between the created plane and this line it means that the point is in the line.
        val intersection = plane.intersectionPhi(this)

        val phi = intersection ?: getPhiForPoint(point)

        return getPosition(phi.coerceIn(phiRange))
    }

    override fun getPosition(phi: Double): Vec3d {
        if (phi !in phiRange)
            throw IllegalArgumentException("Phi must be in range $phiRange")

        return super.getPosition(phi)
    }
}
