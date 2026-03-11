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

    fun intersectionPhi(line: Line): Double? {
        val d = this.pos.dot(this.normalVec)
        val e = line.direction.dot(this.normalVec)

        // If the line is in the plane or parallel to it, there is no intersection point
        if (Mth.equal(e, 0.0)) {
            return null
        }

        val phi = (d - line.position.dot(this.normalVec)) / e

        return phi
    }

    fun intersection(line: Line): Vec3? {
        return intersectionPhi(line)?.let(line::getPositionChecked)
    }

    fun intersection(other: NormalizedPlane): Line? {
        val x1 = other.normalVec.x
        val y1 = other.normalVec.y
        val z1 = other.normalVec.z
        val v1 = other.normalVec.dot(other.pos)

        val x2 = this.normalVec.x
        val y2 = this.normalVec.y
        val z2 = this.normalVec.z
        val v2 = this.normalVec.dot(this.pos)

        val dY = x2 * z1 - x1 * z2
        val dXZ = x2 * y1 - x1 * y2

        when {
            !Mth.equal(dY, 0.0) -> {
                return Line(
                    Vec3(
                        (-v1 * z2 + v2 * z1) / dY,
                        0.0,
                        (v1 * x2 - v2 * x1) / dY
                    ),
                    Vec3(
                        (-z1 * y2 + z2 * y1) / dY,
                        1.0,
                        (x1 * y2 - x2 * y1) / dY,
                    )
                )
            }
            !Mth.equal(dXZ, 0.0) -> {
                return Line(
                    Vec3(
                        (-v1 * z2 + v2 * y1) / dXZ,
                        (v1 * x2 - v2 * x1) / dXZ,
                        0.0
                    ),
                    Vec3(
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
        @JvmStatic
        fun fromPoints(a: Vec3, b: Vec3, c: Vec3): NormalizedPlane {
            val ab = b.subtract(a)
            val ac = c.subtract(a)

            return fromParams(a, ab, ac)
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
