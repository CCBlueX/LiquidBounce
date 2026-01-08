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
package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.world.phys.Vec3
import kotlin.math.absoluteValue

fun getLambda(p: Vec3, u: Vec3, vec: Vec3): Double {
    val uAbs = Vec3(u.x.absoluteValue, u.y.absoluteValue, u.z.absoluteValue)

    val diff = vec - p

    if (uAbs.x > uAbs.y && uAbs.x > uAbs.z) {
        return diff.x / u.x
    }
    if (uAbs.y > uAbs.x && uAbs.y > uAbs.z) {
        return diff.y / u.y
    }

    require(uAbs.z != 0.0)

    return diff.z / u.z
}

/**
 * Determines the squared distance between the straight light defined by the base vector [s] and the direction vector [r] the point [p]
 */
fun straightLinePointDistanceSquared(s: Vec3, r: Vec3, p: Vec3): Double {
    // f: x(v) -> p(v) + lambda(s) * u(v)

    // H: p(v) * n(v) = d(s)
    val planeD = r.dot(p)

    val lambda = (planeD - r.dot(s)) / r.dot(r)

    return (s + r * lambda).distanceToSqr(p)
}

/**
 * Determines the squared distance between the line defined by [l1] and [l2] the point [p]
 */
fun linePointDistanceSquared(l1: Vec3, l2: Vec3, p: Vec3): Double {
    // f: x(v) -> p(v) + lambda(s) * u(v)
    val lU = l2 - l1

    // H: p(v) * n(v) = d(s)
    val planeD = lU.dot(p)

    val lambda = (planeD - lU.dot(l1)) / lU.dot(lU)

    // In this case, the line is limited
    val clampedLambda = lambda.coerceIn(0.0, getLambda(l1, lU, l2))

    return (l1 + lU * clampedLambda).distanceToSqr(p)
}
