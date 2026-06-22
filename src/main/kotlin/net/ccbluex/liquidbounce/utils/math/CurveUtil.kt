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

package net.ccbluex.liquidbounce.utils.math

import net.ccbluex.fastutil.asObjectList
import net.ccbluex.liquidbounce.config.types.list.Tagged
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.component1
import org.joml.component2

/**
 * Chart.js spline interpolation
 */
@Suppress("TooManyFunctions")
object CurveUtil {

    enum class OnOutOfBounds(override val tag: String) : Tagged {
        CLAMP("Clamp"),
        EXTEND("Extend");

        internal fun resolveOutOfBoundsY(
            data: List<Vector2fc>,
            xPos: Float,
            isLeftSide: Boolean
        ): Float {
            return when (this) {
                CLAMP -> if (isLeftSide) data.first().y() else data.last().y()
                EXTEND -> extrapolateLinear(data, xPos, isLeftSide)
            }
        }
    }

    /**
     * Find Y position at a given X using spline interpolation.
     *
     * @param data List of 2D points representing the curve
     * @param xPos X position to sample
     * @param tension Spline tension in range [0, 1] (out-of-range values are normalized)
     * @param onOutOfBounds Behavior for X values outside the curve domain, defaults to [OnOutOfBounds.CLAMP]
     */
    @JvmOverloads
    @JvmStatic
    fun transform(
        data: List<Vector2fc>,
        xPos: Float,
        tension: Float,
        onOutOfBounds: OnOutOfBounds = OnOutOfBounds.CLAMP,
    ): Float {
        require(data.isNotEmpty()) { "Curve data must not be empty" }

        if (data.size == 1) {
            return data[0].y()
        }

        val normalizedData = sortAndDeduplicateByX(data)
        val normalizedTension = normalizeTension(tension)

        return transformNormalized(normalizedData, xPos, normalizedTension, onOutOfBounds)
    }

    @JvmStatic
    internal fun transformNormalized(
        data: List<Vector2fc>,
        xPos: Float,
        tension: Float,
        onOutOfBounds: OnOutOfBounds,
    ): Float {
        if (data.size == 1) {
            return data[0].y()
        }

        val firstPoint = data.first()
        val lastPoint = data.last()

        return when {
            xPos < firstPoint.x() -> onOutOfBounds.resolveOutOfBoundsY(data, xPos, isLeftSide = true)
            xPos > lastPoint.x() -> onOutOfBounds.resolveOutOfBoundsY(data, xPos, isLeftSide = false)
            xPos == firstPoint.x() -> firstPoint.y()
            xPos == lastPoint.x() -> lastPoint.y()
            data.size == 2 -> interpolateLinear(data[0], data[1], xPos)
            else -> findYByExactX(data, xPos) ?: interpolateSpline(data, xPos, tension)
        }
    }

    /**
     * Normalize data by sorting points by X and removing duplicates.
     */
    private fun sortAndDeduplicateByX(data: List<Vector2fc>): List<Vector2fc> {
        if (data.size <= 1) {
            return data
        }

        val points = data.toTypedArray()
        points.sortBy(Vector2fc::x)

        var keptSize = 0
        for (index in points.indices) {
            val point = points[index]
            if (keptSize == 0 || points[keptSize - 1].x() != point.x()) {
                points[keptSize++] = point
            } else {
                points[keptSize - 1] = point
            }
        }

        return points.asObjectList(0, keptSize)
    }

    private fun normalizeTension(tension: Float): Float {
        return if (tension.isInfinite() || tension.isNaN()) 0f else tension.coerceIn(0f, 1f)
    }

    private fun findYByExactX(data: List<Vector2fc>, xPos: Float): Float? =
        data.find { it.x() == xPos }?.y()

    private fun extrapolateLinear(data: List<Vector2fc>, xPos: Float, isLeftSide: Boolean): Float {
        val p0 = if (isLeftSide) data[0] else data[data.lastIndex - 1]
        val p1 = if (isLeftSide) data[1] else data[data.lastIndex]
        return interpolateLinear(p0, p1, xPos)
    }

    private fun interpolateSpline(data: List<Vector2fc>, xPos: Float, tension: Float): Float {
        val (prev, point, next) = findControlPoints(data, xPos)
        val t = calculateT(xPos, point.x(), next.x())

        val (_, p1) = createSplineCurve(prev, point, next, tension)
        val p2 = findSecondControlPoint(data, next, tension)

        return calculateBezierY(t, point, p1, p2, next)
    }

    private fun findControlPoints(data: List<Vector2fc>, xPos: Float): Triple<Vector2fc, Vector2fc, Vector2fc> {
        val adjustedXPos = if (xPos == data.first().x()) xPos + 1f else xPos
        val nextIndex = data.indexOfFirst { it.x() >= adjustedXPos }.let {
            if (it == -1 || adjustedXPos == data.last().x()) data.lastIndex else it
        }

        val prevIndex = (nextIndex - 2).coerceAtLeast(0)
        val currentIndex = (nextIndex - 1).coerceAtLeast(0)

        return Triple(
            data[prevIndex],
            data[currentIndex],
            data.getOrElse(nextIndex) { data.last() }
        )
    }

    private fun calculateT(xPos: Float, leftX: Float, rightX: Float): Float {
        if (xPos == rightX) {
            return 1f
        }

        val delta = rightX - leftX
        if (delta == 0f) {
            return 0f
        }

        return ((xPos - leftX) / delta).coerceIn(0f, 1f)
    }

    private fun createSplineCurve(
        prev: Vector2fc,
        point: Vector2fc,
        next: Vector2fc,
        tension: Float
    ): Pair<Vector2f, Vector2f> {
        val d01 = prev.distance(point)
        val d12 = point.distance(next)
        val totalDistance = d01 + d12

        if (totalDistance == 0f) return Vector2f(point) to Vector2f(point)

        val fa = tension * d01 / totalDistance
        val fb = tension * d12 / totalDistance

        val (diffX, diffY) = Vector2f(next).sub(prev)

        return Vector2f(point).sub(diffX * fa, diffY * fa) to
            Vector2f(point).add(diffX * fb, diffY * fb)
    }

    private fun findSecondControlPoint(data: List<Vector2fc>, currentNext: Vector2fc, tension: Float): Vector2f {
        val nextIndex = data.indexOfFirst { it.x() > currentNext.x() }
        if (nextIndex == -1) return Vector2f(currentNext)

        val segment = data.getOrNull(nextIndex - 1) ?: currentNext
        val nextSegment = data.getOrNull(nextIndex) ?: currentNext
        val diff = nextSegment.x() - segment.x()

        val (prev, point, next) = findControlPoints(data, segment.x() + diff)
        return createSplineCurve(prev, point, next, tension).first
    }

    private fun interpolateLinear(p0: Vector2fc, p1: Vector2fc, xPos: Float): Float {
        val (x0, y0) = p0
        val (x1, y1) = p1
        val deltaX = x1 - x0
        if (deltaX == 0f) {
            return y0
        }

        val t = (xPos - x0) / deltaX
        return y0 + (y1 - y0) * t
    }

    private fun calculateBezierY(t: Float, p0: Vector2fc, p1: Vector2fc, p2: Vector2fc, p3: Vector2fc): Float {
        val t2 = t * t
        val t3 = t2 * t
        val mt = 1f - t
        val mt2 = mt * mt
        val mt3 = mt2 * mt

        return mt3 * p0.y() + 3f * mt2 * t * p1.y() + 3f * mt * t2 * p2.y() + t3 * p3.y()
    }
}
