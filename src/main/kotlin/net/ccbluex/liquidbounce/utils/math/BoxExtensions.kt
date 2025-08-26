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
package net.ccbluex.liquidbounce.utils.math

import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.math.max
import kotlin.math.min

/**
 * Tests if the infinite line resulting from [start] and the point [p] will intersect this box.
 */
fun Box.isHitByLine(start: Vec3d, p: Vec3d): Boolean {
    val d = p.subtract(start)

    var tEntry = Double.NEGATIVE_INFINITY
    var tExit = Double.POSITIVE_INFINITY

    fun Box.checkSide(axis: Direction.Axis, start: Vec3d, d: Vec3d): Boolean {
        val d1 = axis.choose(d.x, d.y, d.z)
        val min = getMin(axis)
        val max = getMax(axis)
        val p0 = axis.choose(start.x, start.y, start.z)

        // parallel and outside, no need to check anything else
        if (d1 == 0.0 && (p0 < min || p0 > max)) {
            return true
        }

        val t1 = (min - p0) / d1
        val t2 = (max - p0) / d1
        val tMin = min(t1, t2)
        val tMax = max(t1, t2)

        tEntry = maxOf(tEntry, tMin)
        tExit = minOf(tExit, tMax)

        return tEntry > tExit
    }

    if (checkSide(Direction.Axis.X, start, d) ||
        checkSide(Direction.Axis.Y, start, d) ||
        checkSide(Direction.Axis.Z, start, d)) {
        return false
    }

    return tEntry <= tExit
}

val Box.size: Double
    get() = this.lengthX * this.lengthY * this.lengthZ

fun Box.getCoordinate(direction: Direction): Double {
    return if (direction.direction == Direction.AxisDirection.POSITIVE) {
        this.getMax(direction.axis)
    } else {
        this.getMin(direction.axis)
    }
}
