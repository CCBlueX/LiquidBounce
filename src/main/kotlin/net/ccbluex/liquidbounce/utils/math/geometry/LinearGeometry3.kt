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

import net.ccbluex.liquidbounce.utils.math.addScaled
import net.ccbluex.liquidbounce.utils.math.distanceToSqr
import net.ccbluex.liquidbounce.utils.math.dot
import net.ccbluex.liquidbounce.utils.math.isLikelyZero
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.function.DoubleConsumer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Shared contract for one-dimensional linear geometry in 3D space.
 *
 * Implementations define their own valid parameter domain, while [anchor] and [direction]
 * always describe the shared supporting line equation `anchor + direction * t`.
 */
sealed interface LinearGeometry3 {

    /**
     * Parameter-zero point of this geometry.
     */
    val anchor: Vec3

    /**
     * Direction vector of the supporting line.
     *
     * This vector is required to be non-zero and is not necessarily normalized.
     */
    val direction: Vec3

    /**
     * Returns the point at [parameter] if it belongs to this geometry's parameter domain.
     *
     * Returns `null` for out-of-domain or non-finite parameters.
     */
    fun pointAtOrNull(parameter: Double): Vec3? {
        val normalized = parameterDomain().normalize(parameter)
        return if (normalized.isNaN()) null else pointAt(normalized)
    }

    /**
     * Returns the nearest point on this geometry to [point].
     */
    fun getNearestPointTo(point: Vec3): Vec3 {
        val parameter = parameterDomain().project(parameterFor(point))
        if (parameter.isNaN()) {
            error("Unable to project point $point on geometry $this")
        }
        return pointAt(parameter)
    }

    /**
     * Returns the squared distance from [point] to this geometry.
     */
    fun distanceToSqr(point: Vec3): Double {
        return getNearestPointTo(point).distanceToSqr(point)
    }

    /**
     * Returns the squared distance from [box] to this geometry.
     */
    fun distanceToSqr(box: AABB): Double {
        val pointOnGeometry = getNearestPointTo(box)
        return box.distanceToSqr(pointOnGeometry)
    }

    /**
     * Returns the nearest point pair between this geometry and [other].
     */
    fun getNearestPointsTo(other: LinearGeometry3): Pair<Vec3, Vec3>? {
        val firstDirection = direction
        val secondDirection = other.direction
        val firstDomain = parameterDomain()
        val secondDomain = other.parameterDomain()

        val deltaX = anchor.x - other.anchor.x
        val deltaY = anchor.y - other.anchor.y
        val deltaZ = anchor.z - other.anchor.z

        val firstDx = firstDirection.x
        val firstDy = firstDirection.y
        val firstDz = firstDirection.z
        val secondDx = secondDirection.x
        val secondDy = secondDirection.y
        val secondDz = secondDirection.z

        val a = firstDirection.dot(firstDirection)
        val b = firstDirection.dot(secondDirection)
        val c = secondDirection.dot(secondDirection)
        val d = firstDirection.dot(deltaX, deltaY, deltaZ)
        val e = secondDirection.dot(deltaX, deltaY, deltaZ)

        val determinant = a * c - b * b
        var bestFirstParameter = Double.NaN
        var bestSecondParameter = Double.NaN
        var bestDistance = Double.POSITIVE_INFINITY

        fun addCandidate(firstParameterCandidate: Double, secondParameterCandidate: Double) {
            val firstParameter = firstDomain.normalize(firstParameterCandidate)
            val secondParameter = secondDomain.normalize(secondParameterCandidate)

            if (firstParameter.isNaN() || secondParameter.isNaN()) {
                return
            }

            val dx = deltaX + firstDx * firstParameter - secondDx * secondParameter
            val dy = deltaY + firstDy * firstParameter - secondDy * secondParameter
            val dz = deltaZ + firstDz * firstParameter - secondDz * secondParameter
            val distance = dx * dx + dy * dy + dz * dz

            if (bestFirstParameter.isNaN() || distance < bestDistance - GEOMETRY_PARAMETER_EPSILON) {
                bestFirstParameter = firstParameter
                bestSecondParameter = secondParameter
                bestDistance = distance
            }
        }

        if (abs(determinant) > GEOMETRY_PARAMETER_EPSILON) {
            val unconstrainedFirst = (b * e - c * d) / determinant
            val unconstrainedSecond = (a * e - b * d) / determinant
            addCandidate(unconstrainedFirst, unconstrainedSecond)
        }

        firstDomain.forEachFiniteBoundary { firstBoundary ->
            addCandidate(firstBoundary, secondDomain.project((b * firstBoundary + e) / c))
        }

        secondDomain.forEachFiniteBoundary { secondBoundary ->
            addCandidate(firstDomain.project((b * secondBoundary - d) / a), secondBoundary)
        }

        addCandidate(firstDomain.project(-d / a), 0.0)
        addCandidate(0.0, secondDomain.project(e / c))

        if (bestFirstParameter.isNaN()) {
            return null
        }

        return pointAt(bestFirstParameter) to other.pointAt(bestSecondParameter)
    }

    /**
     * Returns the nearest point on this geometry to [box].
     */
    @Suppress("CognitiveComplexMethod")
    fun getNearestPointTo(box: AABB): Vec3 {
        val px = anchor.x
        val py = anchor.y
        val pz = anchor.z

        val dx = direction.x
        val dy = direction.y
        val dz = direction.z

        val breakpoints = DoubleArray(6)
        var breakpointCount = 0

        fun addBreakpoint(parameter: Double) {
            if (!parameter.isFinite()) {
                return
            }

            breakpoints[breakpointCount++] = parameter
        }

        if (!Mth.equal(dx, 0.0)) {
            addBreakpoint((box.minX - px) / dx)
            addBreakpoint((box.maxX - px) / dx)
        }
        if (!Mth.equal(dy, 0.0)) {
            addBreakpoint((box.minY - py) / dy)
            addBreakpoint((box.maxY - py) / dy)
        }
        if (!Mth.equal(dz, 0.0)) {
            addBreakpoint((box.minZ - pz) / dz)
            addBreakpoint((box.maxZ - pz) / dz)
        }

        breakpointCount = sortAndUnique(breakpoints, breakpointCount)

        val domain = parameterDomain()
        var bestParameter = Double.NaN
        var bestDistance = Double.POSITIVE_INFINITY

        fun evaluate(parameterCandidate: Double) {
            val parameter = domain.normalize(parameterCandidate)
            if (parameter.isNaN()) {
                return
            }
            val x = px + dx * parameter
            val y = py + dy * parameter
            val z = pz + dz * parameter
            val distance = box.distanceToSqr(x, y, z)

            if (bestParameter.isNaN() || distance < bestDistance - GEOMETRY_PARAMETER_EPSILON) {
                bestParameter = parameter
                bestDistance = distance
            }
        }

        domain.forEachFiniteBoundary(::evaluate)
        for (index in 0 until breakpointCount) {
            evaluate(breakpoints[index])
        }

        val markers = DoubleArray(8)
        var markerCount = 0

        fun addMarker(parameter: Double) {
            if (parameter < domain.lowerBound - GEOMETRY_PARAMETER_EPSILON ||
                parameter > domain.upperBound + GEOMETRY_PARAMETER_EPSILON
            ) {
                return
            }

            markers[markerCount++] = parameter
        }

        for (index in 0 until breakpointCount) {
            addMarker(breakpoints[index])
        }
        domain.forEachFiniteBoundary(::addMarker)

        markerCount = sortAndUnique(markers, markerCount)

        var intervalStart = domain.lowerBound
        if (markerCount == 0) {
            evaluateInterval(box, domain, intervalStart, domain.upperBound, px, py, pz, dx, dy, dz, ::evaluate)
        } else {
            for (index in 0 until markerCount) {
                val marker = markers[index]
                evaluateInterval(box, domain, intervalStart, marker, px, py, pz, dx, dy, dz, ::evaluate)
                intervalStart = marker
            }
            evaluateInterval(box, domain, intervalStart, domain.upperBound, px, py, pz, dx, dy, dz, ::evaluate)
        }

        if (bestParameter.isNaN()) {
            evaluate(0.0)
        }

        if (bestParameter.isNaN()) {
            error("Unable to find nearest point on geometry $this")
        }

        return pointAt(bestParameter)
    }

    /**
     * Returns the point on the supporting line at [parameter].
     *
     * This method does not validate the parameter domain.
     */
    fun pointAt(parameter: Double): Vec3 {
        return anchor.addScaled(direction, parameter)
    }

    /**
     * Returns the unconstrained projection parameter of [point] on the supporting line.
     */
    fun parameterFor(point: Vec3): Double {
        return direction.dot(
            x = point.x - anchor.x,
            y = point.y - anchor.y,
            z = point.z - anchor.z,
        ) / direction.lengthSqr()
    }

    private fun parameterDomain(): ParameterDomain =
        when (this) {
            is Line -> ParameterDomain.UNBOUNDED
            is Ray -> ParameterDomain.FORWARD
            is LineSegment -> ParameterDomain.SEGMENT_01
        }

    private fun evaluateInterval(
        box: AABB,
        domain: ParameterDomain,
        start: Double,
        end: Double,
        px: Double,
        py: Double,
        pz: Double,
        dx: Double,
        dy: Double,
        dz: Double,
        evaluate: DoubleConsumer,
    ) {
        val intervalStart = max(start, domain.lowerBound)
        val intervalEnd = min(end, domain.upperBound)
        val sample = sampleOpenInterval(intervalStart, intervalEnd) ?: return

        val sx = px + dx * sample
        val sy = py + dy * sample
        val sz = pz + dz * sample

        var quadraticA = 0.0
        var quadraticB = 0.0

        fun addAxis(sampleCoord: Double, min: Double, max: Double, dir: Double, pos: Double) {
            if (sampleCoord < min) {
                quadraticA += dir * dir
                quadraticB += dir * (pos - min)
            } else if (sampleCoord > max) {
                quadraticA += dir * dir
                quadraticB += dir * (pos - max)
            }
        }

        addAxis(sx, box.minX, box.maxX, dx, px)
        addAxis(sy, box.minY, box.maxY, dy, py)
        addAxis(sz, box.minZ, box.maxZ, dz, pz)

        if (abs(quadraticA) <= GEOMETRY_PARAMETER_EPSILON) {
            evaluate.accept(sample)
            return
        }

        val root = -quadraticB / quadraticA
        if (root.inOpenInterval(intervalStart, intervalEnd)) {
            evaluate.accept(root)
        }
    }
}

internal const val GEOMETRY_PARAMETER_EPSILON = 1e-9

private enum class ParameterDomain(
    @JvmField val lowerBound: Double,
    @JvmField val upperBound: Double,
) {
    UNBOUNDED(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
    FORWARD(0.0, Double.POSITIVE_INFINITY),
    SEGMENT_01(0.0, 1.0);

    /**
     * @return [Double.NaN] if [parameter] out of bounds
     */
    fun normalize(parameter: Double): Double {
        if (!parameter.isFinite()) {
            return Double.NaN
        }

        if (parameter < lowerBound - GEOMETRY_PARAMETER_EPSILON
            || parameter > upperBound + GEOMETRY_PARAMETER_EPSILON) {
            return Double.NaN
        }

        return parameter.coerceIn(lowerBound, upperBound)
    }

    /**
     * @return [Double.NaN] if [parameter] out of bounds
     */
    fun project(parameter: Double): Double {
        if (!parameter.isFinite()) {
            return Double.NaN
        }

        return parameter.coerceIn(lowerBound, upperBound)
    }

    inline fun forEachFiniteBoundary(action: (Double) -> Unit) {
        when (this) {
            UNBOUNDED -> {}
            FORWARD -> action(0.0)
            SEGMENT_01 -> {
                action(0.0)
                action(1.0)
            }
        }
    }
}

internal fun requireValidDirection(direction: Vec3) {
    require(!direction.isLikelyZero) {
        "Direction should be not zero, actual: $direction"
    }
}

private fun sortAndUnique(values: DoubleArray, size: Int): Int {
    for (index in 1 until size) {
        val value = values[index]
        var insertionIndex = index

        while (insertionIndex > 0 && values[insertionIndex - 1] > value) {
            values[insertionIndex] = values[insertionIndex - 1]
            insertionIndex--
        }

        values[insertionIndex] = value
    }

    if (size == 0) {
        return 0
    }

    var uniqueCount = 1

    for (index in 1 until size) {
        val value = values[index]

        if (value == values[uniqueCount - 1]) {
            continue
        }

        values[uniqueCount++] = value
    }

    return uniqueCount
}

private fun sampleOpenInterval(start: Double, end: Double): Double? {
    if (start.isFinite() && end.isFinite()) {
        return if (start < end - GEOMETRY_PARAMETER_EPSILON) (start + end) * 0.5 else null
    }

    if (start.isFinite()) {
        return start + 1.0
    }

    if (end.isFinite()) {
        return end - 1.0
    }

    return 0.0
}

private fun Double.inOpenInterval(start: Double, end: Double): Boolean {
    if (!isFinite()) {
        return false
    }

    val aboveLower = !start.isFinite() || this > start + GEOMETRY_PARAMETER_EPSILON
    val belowUpper = !end.isFinite() || this < end - GEOMETRY_PARAMETER_EPSILON
    return aboveLower && belowUpper
}
