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

import org.joml.Vector2f

/**
 * Chart.js spline interpolation
 */
object CurveUtil {

    /**
     * Find Y position at a given X using spline interpolation
     * Handles out-of-bounds X values by clamping to the nearest endpoint
     */
    fun transform(data: List<Vector2f>, xPos: Float, tension: Float): Float {
        return when {
            xPos <= data.first().x -> data.first().y
            xPos >= data.last().x -> data.last().y
            else -> interpolateSpline(data, xPos, tension)
        }
    }

    private fun interpolateSpline(data: List<Vector2f>, xPos: Float, tension: Float): Float {
        val (prev, point, next) = findControlPoints(data, xPos)
        val t = calculateT(xPos, point.x, next.x)

        val (_, p1) = createSplineCurve(prev, point, next, tension)
        val p2 = findSecondControlPoint(data, next, tension)

        return calculateBezierY(t, point, p1, p2, next)
    }

    private fun findControlPoints(data: List<Vector2f>, xPos: Float): Triple<Vector2f, Vector2f, Vector2f> {
        val adjustedXPos = if (xPos == data.first().x) xPos + 1f else xPos
        val nextIndex = data.indexOfFirst { it.x >= adjustedXPos }.let {
            if (it == -1 || adjustedXPos == data.last().x) data.lastIndex else it
        }

        val prevIndex = (nextIndex - 2).coerceAtLeast(0)
        val currentIndex = (nextIndex - 1).coerceAtLeast(0)

        return Triple(
            data[prevIndex],
            data[currentIndex],
            data.getOrElse(nextIndex) { data.last() }
        )
    }

    private fun calculateT(xPos: Float, leftX: Float, rightX: Float): Float =
        if (xPos == rightX) 1f else ((xPos - leftX) / (rightX - leftX)).coerceIn(0f, 1f)

    private fun createSplineCurve(
        prev: Vector2f,
        point: Vector2f,
        next: Vector2f,
        tension: Float
    ): Pair<Vector2f, Vector2f> {
        val d01 = prev.distance(point)
        val d12 = point.distance(next)
        val totalDistance = d01 + d12

        if (totalDistance == 0f) return Vector2f(point) to Vector2f(point)

        val fa = tension * d01 / totalDistance
        val fb = tension * d12 / totalDistance

        val diff = Vector2f(next).sub(prev)

        return Vector2f(point).sub(Vector2f(diff).mul(fa)) to
            Vector2f(point).add(Vector2f(diff).mul(fb))
    }

    private fun findSecondControlPoint(data: List<Vector2f>, currentNext: Vector2f, tension: Float): Vector2f {
        val nextIndex = data.indexOfFirst { it.x > currentNext.x }
        if (nextIndex == -1) return Vector2f(currentNext)

        val segment = data.getOrNull(nextIndex - 1) ?: currentNext
        val nextSegment = data.getOrNull(nextIndex) ?: currentNext
        val diff = nextSegment.x - segment.x

        val (prev, point, next) = findControlPoints(data, segment.x + diff)
        return createSplineCurve(prev, point, next, tension).first
    }

    private fun calculateBezierY(t: Float, p0: Vector2f, p1: Vector2f, p2: Vector2f, p3: Vector2f): Float {
        val t2 = t * t
        val t3 = t2 * t
        val mt = 1f - t
        val mt2 = mt * mt
        val mt3 = mt2 * mt

        return mt3 * p0.y + 3f * mt2 * t * p1.y + 3f * mt * t2 * p2.y + t3 * p3.y
    }
}
