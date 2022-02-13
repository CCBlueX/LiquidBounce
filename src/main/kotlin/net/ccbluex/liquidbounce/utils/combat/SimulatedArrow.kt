/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.sqrt

enum class ArrowSimulationResultType {
    Hit,
    Miss,
    WrongDirection
}

fun simulate(pos: Vec3d, velocity: Vec3d, target: Vec3d): Pair<Vec3d, Double> {
    var pos = pos
    var velocity = velocity

    var minPos = pos
    var minDist = Double.MAX_VALUE

    for (i in 0 until 20 * 20) {
        val newPos = pos + velocity

        val drag = if (isTouchingWater()) {
            0.6
        } else {
            0.99
        }

        velocity *= drag

        velocity.y -= 0.05000000074505806


        if (velocity.y < 0.0) {
            val dist = linePointDistanceSquared(pos, newPos, target)

            if (dist < minDist) {
                minDist = dist
                minPos = pos
            }

            if (newPos.y < target.y)
                return Pair(minPos, minDist)
        }

        pos = newPos
    }

    throw IllegalStateException()
}

fun approximatePitch(
    velocity: Float,
    yaw: Float,
    target: Vec3d,
    accuracy: Float = 0.05F / 180.0F * Math.PI.toFloat()
): Float {
    var maxPitch = Math.PI.toFloat()
    var minPitch = -Math.PI.toFloat()

    var pitch = minPitch + (maxPitch - minPitch) / 2.0F

    val iterations = ceil(log2(Math.PI.toFloat() / accuracy * 2.0F)).toInt()

    for (i in 0 until iterations) {
        assert(maxPitch > minPitch)

        pitch = minPitch + (maxPitch - minPitch) / 2.0F

        if (maxPitch - minPitch <= accuracy) {
            return pitch
        }

        val vX = -MathHelper.sin(yaw * 0.017453292f) * MathHelper.cos(pitch * 0.017453292f)
        val vY = -MathHelper.sin(pitch * 0.017453292f)
        val vZ = MathHelper.cos(yaw * 0.017453292f) * MathHelper.cos(pitch * 0.017453292f)

        val simulationResult = simulate(Vec3d(0.0, 0.0, 0.0), Vec3d(vX.toDouble(), vY.toDouble(), vZ.toDouble()) * velocity.toDouble(), target)

        val targetLanding = simulationResult.first - target

        val cathetus = (targetLanding.x * target.x + targetLanding.z * target.z) / sqrt((targetLanding.x * targetLanding.x + targetLanding.z * targetLanding.z) * (target.x * target.x + target.z * target.z))

        println("$cathetus/${simulationResult.first}/${simulationResult.second}")

        if (cathetus > 0.0) {
            minPitch = pitch
        } else {
            maxPitch = pitch
        }
    }

    return pitch
}

fun getLambda(p: Vec3d, u: Vec3d, vec: Vec3d): Double {
    val uAbs = Vec3d(u.x.absoluteValue, u.y.absoluteValue, u.z.absoluteValue)

    val diff = vec - p

    if (uAbs.x > uAbs.y && uAbs.x > uAbs.z) {
        return diff.x / u.x
    }
    if (uAbs.y > uAbs.x && uAbs.y > uAbs.z) {
        return diff.y / u.y
    }

    if (uAbs.z == 0.0) {
        throw IllegalArgumentException()
    }

    return diff.z / u.z
}

/**
 * Determines the distance between the line defined by [l1] and [l2] the point [p]
 */
fun linePointDistanceSquared(l1: Vec3d, l2: Vec3d, p: Vec3d): Double {
    // f: x(v) -> p(v) + lambda(s) * u(v)
    val lP = l1
    val lU = l2 - l1

    // H: p(v) * n(v) = d(s)
    val planeN = lU
    val planeD = lU.dotProduct(p)

    val lambda = (planeD - planeN.dotProduct(lP)) / planeN.dotProduct(lU)

    // In this case, the line is limited
    val clampedLambda = lambda.coerceIn(0.0, getLambda(lP, lU, l2))

    return (lP + lU * clampedLambda).squaredDistanceTo(p)
}

fun isTouchingWater(): Boolean = false
