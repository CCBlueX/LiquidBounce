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

import net.ccbluex.liquidbounce.utils.math.isLikelyZero
import net.ccbluex.liquidbounce.utils.math.normalizeIfNeeded
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

class NormalizedPlane(val pos: Vec3, normalVec: Vec3) {

    val normalVec: Vec3 = normalVec.normalizeIfNeeded()

    fun intersectionPhi(geometry: LinearGeometry3): Double? {
        val d = pos.dot(normalVec)
        val e = geometry.direction.dot(normalVec)

        if (Mth.equal(e, 0.0)) {
            return null
        }

        return (d - geometry.anchor.dot(normalVec)) / e
    }

    fun intersection(geometry: LinearGeometry3): Vec3? {
        return intersectionPhi(geometry)?.let(geometry::pointAtOrNull)
    }

    fun intersection(other: NormalizedPlane): Line? {
        val firstNormal = other.normalVec
        val secondNormal = normalVec
        val direction = firstNormal.cross(secondNormal)
        val directionLengthSqr = direction.lengthSqr()

        if (Mth.equal(directionLengthSqr, 0.0)) {
            return null
        }

        val firstDistance = firstNormal.dot(other.pos)
        val secondDistance = secondNormal.dot(pos)

        val point = secondNormal.cross(direction).scale(firstDistance)
            .add(direction.cross(firstNormal).scale(secondDistance))
            .scale(1.0 / directionLengthSqr)

        return Line(point, direction)
    }

    companion object {
        @JvmStatic
        fun fromPoints(a: Vec3, b: Vec3, c: Vec3): NormalizedPlane {
            return fromParams(a, b.subtract(a), c.subtract(a))
        }

        @JvmStatic
        fun fromParams(base: Vec3, directionA: Vec3, directionB: Vec3): NormalizedPlane {
            val normalVec = directionA.cross(directionB).normalize()

            require(!normalVec.isLikelyZero) {
                "Points must not be on the same line"
            }

            return NormalizedPlane(base, normalVec)
        }
    }
}
