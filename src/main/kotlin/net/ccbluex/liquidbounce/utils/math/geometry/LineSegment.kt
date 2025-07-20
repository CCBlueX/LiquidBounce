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

import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

class LineSegment(position: Vec3d, direction: Vec3d, val phiRange: ClosedFloatingPointRange<Double>) :
    Line(position, direction) {
    val length: Double
        get() = direction.multiply(phiRange.endInclusive - phiRange.start).length()

    val endPoints: Pair<Vec3d, Vec3d>
        get() = Pair(getPosition(phiRange.start), getPosition(phiRange.endInclusive))

    init {
        require(!MathHelper.approximatelyEquals(direction.lengthSquared(), 0.0)) {
            "Direction must not be zero"
        }
    }

    override fun getNearestPointTo(point: Vec3d): Vec3d {
        val plane = NormalizedPlane(point, direction)

        // If there is no intersection between the created plane and this line it means that the point is in the line.
        val intersection = plane.intersectionPhi(this)

        val phi = intersection ?: getPhiForPoint(point)

        return getPosition(phi.coerceIn(phiRange))
    }

    override fun calculateNearestPhiTo(other: Line): Double? {
        return super.calculateNearestPhiTo(other)?.coerceIn(phiRange)
    }

    override fun getPosition(phi: Double): Vec3d {
        require(phi in phiRange) {
            "Phi must be in range $phiRange"
        }

        return super.getPosition(phi)
    }

    override fun getPositionChcked(phi: Double): Vec3d? {
        if (phi !in phiRange) {
            return null
        }

        return super.getPosition(phi)
    }

    companion object {
        fun fromPoints(a: Vec3d, b: Vec3d): LineSegment {
            return LineSegment(a, b.subtract(a), 0.0..1.0)
        }
    }
}
