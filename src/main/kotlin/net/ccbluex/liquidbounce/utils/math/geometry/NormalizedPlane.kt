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

class NormalizedPlane(val pos: Vec3d, val normalVec: Vec3d) {

    fun intersectionPhi(line: Line): Double? {
        val d = this.pos.dotProduct(this.normalVec)
        val e = line.direction.dotProduct(this.normalVec)

        // If the line is in the plane or parallel to it, there is no intersection point
        if (MathHelper.approximatelyEquals(e, 0.0)) {
            return null
        }

        val phi = (d - line.position.dotProduct(this.normalVec)) / e

        return phi
    }

    fun intersection(line: Line): Vec3d? {
        return intersectionPhi(line)?.let(line::getPositionChcked)
    }

    fun intersection(other: NormalizedPlane): Line? {
        val x1 = other.normalVec.x
        val y1 = other.normalVec.y
        val z1 = other.normalVec.z
        val v1 = other.normalVec.dotProduct(other.pos)

        val x2 = this.normalVec.x
        val y2 = this.normalVec.y
        val z2 = this.normalVec.z
        val v2 = this.normalVec.dotProduct(this.pos)

        val dY = x2 * z1 - x1 * z2
        val dXZ = x2 * y1 - x1 * y2

        when {
            !MathHelper.approximatelyEquals(dY, 0.0) -> {
                return Line(
                    Vec3d(
                        (-v1 * z2 + v2 * z1) / dY,
                        0.0,
                        (v1 * x2 - v2 * x1) / dY
                    ),
                    Vec3d(
                        (-z1 * y2 + z2 * y1) / dY,
                        1.0,
                        (x1 * y2 - x2 * y1) / dY,
                    )
                )
            }
            !MathHelper.approximatelyEquals(dXZ, 0.0) -> {
                return Line(
                    Vec3d(
                        (-v1 * z2 + v2 * y1) / dXZ,
                        (v1 * x2 - v2 * x1) / dXZ,
                        0.0
                    ),
                    Vec3d(
                        (-y1 * z2 + y2 * z1) / dXZ,
                        (x1 * z2 - x2 * z1) / dXZ,
                        1.0,
                    )
                )
            }
            else -> return null
        }
    }

    companion object {
        fun fromPoints(a: Vec3d, b: Vec3d, c: Vec3d): NormalizedPlane {
            val ab = b.subtract(a)
            val ac = c.subtract(a)

            return fromParams(a, ab, ac)
        }

        fun fromParams(base: Vec3d, directionA: Vec3d, directionB: Vec3d): NormalizedPlane {
            val normalVec = directionA.crossProduct(directionB).normalize()

            require(!MathHelper.approximatelyEquals(normalVec.lengthSquared(), 0.0)) {
                "Points must not be on the same line"
            }

            return NormalizedPlane(base, normalVec)
        }
    }

}
