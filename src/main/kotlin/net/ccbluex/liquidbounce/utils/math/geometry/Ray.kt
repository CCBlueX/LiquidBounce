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

import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min

data class Ray(
    val origin: Vec3,
    override val direction: Vec3,
) : LinearGeometry3 {

    init {
        requireValidDirection(direction)
    }

    override val anchor: Vec3
        get() = origin

    fun firstIntersectionWith(box: AABB): Vec3? {
        var enter = Double.NEGATIVE_INFINITY
        var exit = Double.POSITIVE_INFINITY

        fun updateAxis(origin: Double, direction: Double, minBound: Double, maxBound: Double): Boolean {
            if (Mth.equal(direction, 0.0)) {
                return origin in minBound..maxBound
            }

            val t1 = (minBound - origin) / direction
            val t2 = (maxBound - origin) / direction
            val axisEnter = min(t1, t2)
            val axisExit = max(t1, t2)

            enter = max(enter, axisEnter)
            exit = min(exit, axisExit)
            return enter <= exit + GEOMETRY_PARAMETER_EPSILON
        }

        if (!updateAxis(origin.x, direction.x, box.minX, box.maxX) ||
            !updateAxis(origin.y, direction.y, box.minY, box.maxY) ||
            !updateAxis(origin.z, direction.z, box.minZ, box.maxZ)
        ) {
            return null
        }

        if (exit < 0.0) {
            return null
        }

        val parameter = if (enter >= 0.0) enter else exit
        return pointAtOrNull(parameter)
    }

    companion object {
        @JvmStatic
        fun fromPoints(begin: Vec3, end: Vec3): Ray {
            return Ray(begin, end.subtract(begin))
        }
    }
}
