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

import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.distanceToSqr
import net.ccbluex.liquidbounce.utils.math.dot
import net.ccbluex.liquidbounce.utils.math.isLikelyZero
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@JvmRecord
data class NearestPointResult(
    val point: Vec3,
    val distanceSquared: Double,
)

/**
 * Shared contract for one-dimensional linear geometry in 3D space.
 *
 * Implementations define their own valid parameter domain, while [anchor] and [direction]
 * always describe the shared supporting line equation `anchor + direction * t`.
 */
@Suppress("TooManyFunctions")
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
        check(!parameter.isNaN()) {
            "Unable to project point $point on geometry $this"
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
     * Returns the nearest point on this geometry to [shape].
     *
     * The [shape] is expected to already be expressed in the same coordinate system as this geometry.
     * For block-local shapes, call [net.minecraft.world.phys.shapes.VoxelShape.move] before passing them in.
     *
     * @return nearest point on this geometry together with squared distance, or `null` if [shape] is empty
     */
    fun getNearestPointTo(shape: VoxelShape): NearestPointResult? {
        if (shape.isEmpty) {
            return null
        }

        var bestResult: NearestPointResult? = null

        shape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
            val box = AABB(minX, minY, minZ, maxX, maxY, maxZ)
            val result = getNearestPointTo(box)
            val currentBest = bestResult

            if (currentBest == null ||
                result.distanceSquared < currentBest.distanceSquared - GEOMETRY_PARAMETER_EPSILON) {
                bestResult = result
            }
        }

        return checkNotNull(bestResult) { "Unable to find nearest point on geometry $this" }
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

        // `determinant` is (|firstDirection| * |secondDirection| * sin(angle))², so it has to be compared against a
        // share of `a * c` — against an absolute epsilon every sufficiently short pair counts as parallel and the
        // unconstrained minimum would be skipped.
        if (determinant > PARALLEL_SINE_SQUARED_EPSILON * a * c) {
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
     * Returns whether this geometry intersects [box] within its parameter domain.
     */
    fun intersects(box: AABB): Boolean {
        return boxIntersectionInterval(box) != null
    }

    /**
     * Returns the first boundary intersection with [box] in parameter order.
     *
     * When the geometry starts inside the box, this returns the boundary point where it leaves the box.
     * If the geometry never reaches a box boundary inside its parameter domain, this returns `null`.
     */
    fun firstIntersectionWith(box: AABB): Vec3? {
        val interval = boxIntersectionInterval(box) ?: return null
        val parameter = firstIntersectionParameter(interval)
        return if (parameter.isNaN()) null else pointAtOrNull(parameter)
    }

    /**
     * Returns the nearest point on this geometry to [box].
     */
    fun getNearestPointTo(box: AABB): NearestPointResult {
        val position = anchor
        val directionVector = direction
        val px = position.x
        val py = position.y
        val pz = position.z

        val dx = directionVector.x
        val dy = directionVector.y
        val dz = directionVector.z

        val domain = parameterDomain()
        val candidates = DoubleArray(MAX_CANDIDATE_COUNT)
        val markerCount = candidates.collectMarkers(box, domain, position, directionVector)
        val candidateCount = candidates.addIntervalCandidates(markerCount, box, domain, position, directionVector)

        var bestParameter = Double.NaN
        var bestDistance = Double.POSITIVE_INFINITY

        for (index in 0 until candidateCount) {
            val parameter = domain.normalize(candidates[index])
            if (parameter.isNaN()) {
                continue
            }

            val distance = box.distanceToSqr(px.fma(dx, parameter), py.fma(dy, parameter), pz.fma(dz, parameter))
            if (bestParameter.isNaN() || distance < bestDistance - GEOMETRY_PARAMETER_EPSILON) {
                bestParameter = parameter
                bestDistance = distance
            }
        }

        check(!bestParameter.isNaN()) {
            "Unable to find nearest point on geometry $this"
        }

        return NearestPointResult(
            point = pointAt(bestParameter),
            distanceSquared = bestDistance,
        )
    }

    /**
     * Returns the point on the supporting line at [parameter].
     *
     * This method does not validate the parameter domain.
     */
    fun pointAt(parameter: Double): Vec3 {
        return anchor.fma(parameter, direction)
    }

    /**
     * Returns the unconstrained projection parameter of [point] on the supporting line.
     */
    fun parameterFor(point: Vec3): Double = parameterFor(point.x, point.y, point.z)

    /**
     * Returns the unconstrained projection parameter of input position on the supporting line.
     */
    fun parameterFor(x: Double, y: Double, z: Double): Double {
        return direction.dot(
            x = x - anchor.x,
            y = y - anchor.y,
            z = z - anchor.z,
        ) / direction.lengthSqr()
    }

    private fun parameterDomain(): ParameterDomain =
        when (this) {
            is Line -> ParameterDomain.UNBOUNDED
            is Ray -> ParameterDomain.FORWARD
            is LineSegment -> ParameterDomain.SEGMENT_01
        }

    private fun boxIntersectionInterval(box: AABB): BoxIntersectionInterval? {
        var enter = Double.NEGATIVE_INFINITY
        var exit = Double.POSITIVE_INFINITY

        if (!Direction.Axis.VALUES.all { axis ->
            val position = anchor[axis]
            val delta = direction[axis]
            val minBound = box.min(axis)
            val maxBound = box.max(axis)

            if (Mth.equal(delta, 0.0)) {
                return@all position in minBound..maxBound
            }

            val t1 = (minBound - position) / delta
            val t2 = (maxBound - position) / delta
            val axisEnter = min(t1, t2)
            val axisExit = max(t1, t2)

            enter = max(enter, axisEnter)
            exit = min(exit, axisExit)
            enter <= exit + GEOMETRY_PARAMETER_EPSILON
        }) {
            return null
        }

        val domain = parameterDomain()
        if (exit < domain.lowerBound - GEOMETRY_PARAMETER_EPSILON ||
            enter > domain.upperBound + GEOMETRY_PARAMETER_EPSILON
        ) {
            return null
        }

        return BoxIntersectionInterval(enter = enter, exit = exit)
    }

    private fun firstIntersectionParameter(interval: BoxIntersectionInterval): Double {
        val domain = parameterDomain()
        val firstParameter = max(interval.enter, domain.lowerBound)
        val lowerBoundInsideBox = domain.lowerBound.isFinite() &&
            domain.lowerBound > interval.enter + GEOMETRY_PARAMETER_EPSILON &&
            domain.lowerBound < interval.exit - GEOMETRY_PARAMETER_EPSILON

        if (lowerBoundInsideBox) {
            return domain.normalize(interval.exit)
        }

        return domain.normalize(firstParameter)
    }
}

private const val GEOMETRY_PARAMETER_EPSILON = 1e-9

/**
 * Squared sine of the angle below which two directions are treated as parallel.
 */
private const val PARALLEL_SINE_SQUARED_EPSILON = 1e-9

/**
 * Two plane crossings per axis, the two finite domain boundaries and one interval candidate for every marker
 * plus the trailing interval.
 */
private const val MAX_CANDIDATE_COUNT = 2 * 3 + 2 + (2 * 3 + 2 + 1)

private class BoxIntersectionInterval(
    @JvmField val enter: Double,
    @JvmField val exit: Double,
)

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

/**
 * Appends [parameter] to this candidate buffer if it lies inside [domain], accepting the same
 * [GEOMETRY_PARAMETER_EPSILON] of slack as [ParameterDomain.normalize].
 *
 * @return the number of candidates collected so far
 */
private fun DoubleArray.addCandidate(parameter: Double, count: Int, domain: ParameterDomain): Int {
    if (!parameter.isFinite()) {
        return count
    }

    if (parameter < domain.lowerBound - GEOMETRY_PARAMETER_EPSILON ||
        parameter > domain.upperBound + GEOMETRY_PARAMETER_EPSILON
    ) {
        return count
    }

    this[count] = parameter
    return count + 1
}

/**
 * Collects the parameters at which the geometry crosses a face plane of [box] together with the finite boundaries
 * of [domain], sorted and deduplicated.
 *
 * @return the number of markers written to this buffer
 */
private fun DoubleArray.collectMarkers(
    box: AABB,
    domain: ParameterDomain,
    position: Vec3,
    direction: Vec3,
): Int {
    var count = 0

    for (axis in Direction.Axis.VALUES) {
        val directionCoordinate = direction[axis]

        if (Mth.equal(directionCoordinate, 0.0)) {
            continue
        }

        count = addCandidate((box.min(axis) - position[axis]) / directionCoordinate, count, domain)
        count = addCandidate((box.max(axis) - position[axis]) / directionCoordinate, count, domain)
    }

    if (domain.lowerBound.isFinite()) {
        count = addCandidate(domain.lowerBound, count, domain)
    }
    if (domain.upperBound.isFinite()) {
        count = addCandidate(domain.upperBound, count, domain)
    }

    return sortAndUnique(this, count)
}

/**
 * Appends the parameter inside ([start], [end]) that minimizes the squared distance to [box].
 *
 * The set of box faces the geometry is outside of is constant in between two consecutive crossings, which makes
 * the squared distance a quadratic there and its minimum solvable in closed form.
 *
 * @return the number of candidates collected so far
 */
private fun DoubleArray.addIntervalCandidate(
    start: Double,
    end: Double,
    count: Int,
    box: AABB,
    domain: ParameterDomain,
    position: Vec3,
    direction: Vec3,
): Int {
    val intervalStart = max(start, domain.lowerBound)
    val intervalEnd = min(end, domain.upperBound)
    val sample = sampleOpenInterval(intervalStart, intervalEnd)
    if (sample.isNaN()) {
        return count
    }

    var quadraticA = 0.0
    var quadraticB = 0.0

    for (axis in Direction.Axis.VALUES) {
        val sampleCoordinate = position[axis] + direction[axis] * sample
        val directionCoordinate = direction[axis]
        val positionCoordinate = position[axis]

        if (sampleCoordinate < box.min(axis)) {
            quadraticA += directionCoordinate * directionCoordinate
            quadraticB += directionCoordinate * (positionCoordinate - box.min(axis))
        } else if (sampleCoordinate > box.max(axis)) {
            quadraticA += directionCoordinate * directionCoordinate
            quadraticB += directionCoordinate * (positionCoordinate - box.max(axis))
        }
    }

    val parameter = if (abs(quadraticA) <= GEOMETRY_PARAMETER_EPSILON) {
        sample
    } else {
        val root = -quadraticB / quadraticA
        if (!root.inOpenInterval(intervalStart, intervalEnd)) {
            return count
        }

        root
    }

    this[count] = parameter
    return count + 1
}

/**
 * Appends the parameter minimizing the squared distance to [box] inside every interval in between the [markerCount]
 * markers already collected in this buffer.
 *
 * @return the number of candidates collected so far
 */
private fun DoubleArray.addIntervalCandidates(
    markerCount: Int,
    box: AABB,
    domain: ParameterDomain,
    position: Vec3,
    direction: Vec3,
): Int {
    var count = markerCount
    var intervalStart = domain.lowerBound

    for (index in 0 until markerCount) {
        val marker = this[index]
        count = addIntervalCandidate(intervalStart, marker, count, box, domain, position, direction)
        intervalStart = marker
    }

    return addIntervalCandidate(intervalStart, domain.upperBound, count, box, domain, position, direction)
}

private fun sortAndUnique(values: DoubleArray, size: Int): Int {
    if (size <= 1) {
        return size
    }

    for (index in 1 until size) {
        val value = values[index]
        var insertionIndex = index

        while (insertionIndex > 0 && values[insertionIndex - 1] > value) {
            values[insertionIndex] = values[insertionIndex - 1]
            insertionIndex--
        }

        values[insertionIndex] = value
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

private fun sampleOpenInterval(start: Double, end: Double): Double {
    if (start.isFinite() && end.isFinite()) {
        return if (start < end - GEOMETRY_PARAMETER_EPSILON) (start + end) * 0.5 else Double.NaN
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
