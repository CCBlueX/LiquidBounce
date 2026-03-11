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

import net.minecraft.world.phys.Vec3

class LineSegment(position: Vec3, direction: Vec3, val phiRange: ClosedFloatingPointRange<Double>) :
    Line(position, direction) {
    val length: Double
        get() = direction.scale(phiRange.endInclusive - phiRange.start).length()

    val endPoints: Pair<Vec3, Vec3>
        get() = Pair(getPosition(phiRange.start), getPosition(phiRange.endInclusive))

    override fun getNearestPointTo(point: Vec3): Vec3 {
        val plane = NormalizedPlane(point, direction)

        // If there is no intersection between the created plane and this line it means that the point is in the line.
        val intersection = plane.intersectionPhi(this)

        val phi = intersection ?: getPhiForPoint(point)

        return getPosition(phi.coerceIn(phiRange))
    }

    override fun calculateNearestPhiTo(other: Line): Double? {
        return super.calculateNearestPhiTo(other)?.coerceIn(phiRange)
    }

    override fun getPosition(phi: Double): Vec3 {
        require(phi in phiRange) {
            "Phi must be in range $phiRange"
        }

        return super.getPosition(phi)
    }

    override fun getPositionChecked(phi: Double): Vec3? {
        if (phi !in phiRange) {
            return null
        }

        return super.getPosition(phi)
    }

    companion object {
        @JvmStatic
        fun fromPoints(a: Vec3, b: Vec3): LineSegment {
            return LineSegment(a, b.subtract(a), 0.0..1.0)
        }
    }
}
