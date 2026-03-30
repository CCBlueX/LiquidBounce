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

import net.ccbluex.fastutil.forEachDouble
import net.ccbluex.fastutil.step
import net.ccbluex.liquidbounce.test.assertVec3Equals
import net.ccbluex.liquidbounce.utils.math.getNearestPoint
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LineTest {

    private val eps = 1e-9

    @Test
    fun `nearest point is on line and inside box when line intersects box`() {
        val line = Line(position = Vec3(-2.0, 0.5, 0.5), direction = Vec3.X_AXIS)
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val nearest = line.getNearestPointTo(box)

        assertPointOnGeometry(line, nearest.point)
        assertTrue(box.contains(nearest.point), "Expected nearest point to be inside box, got ${nearest.point}")
        assertEquals(0.0, nearest.distanceSquared, 1e-9)
    }

    @Test
    fun `nearest point for non intersecting line minimizes distance to box`() {
        val line = Line(position = Vec3(2.0, 2.0, 2.0), direction = Vec3(1.0, 1.0, 0.0))
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val nearest = line.getNearestPointTo(box)

        assertPointOnGeometry(line, nearest.point)
        val nearestDistance = nearest.distanceSquared
        assertEquals(1.0, nearestDistance, 1e-8)
        assertNearSampledMinimum(line, box, nearestDistance, -5.0..5.0, 0.01)
    }

    @Test
    fun `ray uses forward only parameter domain`() {
        val ray = Ray(origin = Vec3(1.0, 2.0, 3.0), direction = Vec3(2.0, 0.0, 0.0))

        assertNull(ray.pointAtOrNull(-0.1))
        assertVec3Equals(Vec3(2.0, 2.0, 3.0), ray.pointAtOrNull(0.5)!!, 1e-9)
    }

    @Test
    fun `ray first intersection uses positive boundary hit`() {
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        val rayFromOutside = Ray(Vec3(-2.0, 0.5, 0.5), Vec3.X_AXIS)
        val rayFromInside = Ray(Vec3(0.25, 0.5, 0.5), Vec3.X_AXIS)

        assertVec3Equals(Vec3(0.0, 0.5, 0.5), rayFromOutside.firstIntersectionWith(box)!!, 1e-9)
        assertVec3Equals(Vec3(1.0, 0.5, 0.5), rayFromInside.firstIntersectionWith(box)!!, 1e-9)
    }

    @Test
    fun `line first intersection keeps whole supporting line semantics`() {
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        val line = Line(Vec3(2.0, 0.5, 0.5), Vec3.X_AXIS)
        val ray = Ray(Vec3(2.0, 0.5, 0.5), Vec3.X_AXIS)
        val miss = Line(Vec3(-2.0, 1.5, 0.5), Vec3.X_AXIS)

        assertVec3Equals(Vec3(0.0, 0.5, 0.5), line.firstIntersectionWith(box)!!, 1e-9)
        assertNull(ray.firstIntersectionWith(box))
        assertNull(miss.firstIntersectionWith(box))
        assertTrue(line.intersects(box))
        assertFalse(ray.intersects(box))
        assertFalse(miss.intersects(box))
    }

    @Test
    fun `line segment first intersection follows segment domain`() {
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        val entering = LineSegment(Vec3(-2.0, 0.5, 0.5), Vec3(0.25, 0.5, 0.5))
        val exiting = LineSegment(Vec3(0.25, 0.5, 0.5), Vec3(2.0, 0.5, 0.5))
        val contained = LineSegment(Vec3(0.25, 0.5, 0.5), Vec3(0.75, 0.5, 0.5))

        assertVec3Equals(Vec3(0.0, 0.5, 0.5), entering.firstIntersectionWith(box)!!, 1e-9)
        assertVec3Equals(Vec3(1.0, 0.5, 0.5), exiting.firstIntersectionWith(box)!!, 1e-9)
        assertNull(contained.firstIntersectionWith(box))
        assertTrue(contained.intersects(box))
    }

    @Test
    fun `line segment uses normalized parameter space`() {
        val segment = LineSegment(start = Vec3(2.0, 2.0, 2.0), end = Vec3(3.0, 2.0, 2.0))

        assertVec3Equals(segment.start, segment.pointAtOrNull(0.0)!!, 1e-9)
        assertVec3Equals(segment.end, segment.pointAtOrNull(1.0)!!, 1e-9)
        assertNull(segment.pointAtOrNull(1.5))
        assertNull(segment.pointAtOrNull(-0.1))
    }

    @Test
    fun `line segment nearest point can clamp to endpoint`() {
        val segment = LineSegment(
            start = Vec3(2.0, 2.0, 2.0),
            end = Vec3(3.0, 2.0, 2.0)
        )
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val nearest = segment.getNearestPointTo(box)

        assertPointOnGeometry(segment, nearest.point)
        assertVec3Equals(segment.start, nearest.point, 1e-8)
    }

    @Test
    fun `line starting inside box has zero distance`() {
        val line = Line(position = Vec3(0.25, 0.25, 0.25), direction = Vec3(1.0, 2.0, 3.0))
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val nearest = line.getNearestPointTo(box)

        assertPointOnGeometry(line, nearest.point)
        assertTrue(box.contains(nearest.point), "Expected nearest point to stay in box, got ${nearest.point}")
        assertEquals(0.0, nearest.distanceSquared, 1e-9)
    }

    @Test
    fun `nearest point to box includes squared distance`() {
        val line = Line(position = Vec3(2.0, 2.0, 2.0), direction = Vec3.Z_AXIS)
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val nearest = line.getNearestPointTo(box)
        val pointOnBox = box.getNearestPoint(nearest.point)

        assertPointOnGeometry(line, nearest.point)
        assertPointInOrOnBox(pointOnBox, box)
        assertEquals(nearest.point.distanceToSqr(pointOnBox), nearest.distanceSquared, 1e-8)
    }

    @Test
    fun `nearest point to voxel shape checks each component box instead of overall bounds`() {
        val line = Line(position = Vec3(2.0, 0.5, -3.0), direction = Vec3.Z_AXIS)
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0),
            Shapes.box(3.0, 3.0, 0.0, 4.0, 4.0, 1.0),
        )

        val nearest = requireNotNull(line.getNearestPointTo(shape))
        val distance = shape.closestPointTo(nearest.point)
            .orElse(null)?.distanceToSqr(nearest.point) ?: Double.MAX_VALUE

        assertPointOnGeometry(line, nearest.point)
        assertEquals(1.0, distance, 1e-8)
        assertEquals(distance, nearest.distanceSquared, 1e-8)
    }

    @Test
    fun `nearest point to voxel shape includes squared distance`() {
        val line = Line(position = Vec3(2.0, 0.5, -3.0), direction = Vec3.Z_AXIS)
        val shape = Shapes.or(
            Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0),
            Shapes.box(3.0, 3.0, 0.0, 4.0, 4.0, 1.0),
        )

        val nearest = requireNotNull(line.getNearestPointTo(shape))
        val pointOnShape = shape.closestPointTo(nearest.point).orElse(null) ?: error("Expected non-empty shape")

        assertPointOnGeometry(line, nearest.point)
        assertEquals(nearest.point.distanceToSqr(pointOnShape), nearest.distanceSquared, 1e-8)
    }

    @Test
    fun `empty voxel shape returns null nearest point result`() {
        val line = Line(position = Vec3.ZERO, direction = Vec3.X_AXIS)

        assertNull(line.getNearestPointTo(Shapes.empty()))
    }

    @Test
    fun `segment segment nearest points match sampled minimum on regression case`() {
        val first = LineSegment(
            start = Vec3(-0.6781892221070138, 1.1457456837801203, 1.1953346780518554),
            end = Vec3(-2.872117964915719, 2.6895555516929814, 1.1594944944050667)
        )
        val second = LineSegment(
            start = Vec3(1.8641561164353186, -0.2706235017594425, -2.1760422778755952),
            end = Vec3(4.450931238944055, -2.3598211303341, -4.671970047762581)
        )

        val (nearestOnFirst, nearestOnSecond) = first.getNearestPointsTo(second)!!
        val newDistance = nearestOnFirst.distanceToSqr(nearestOnSecond)
        val sampledMinimum = sampleSegmentDistanceSqr(first, second, 0.005)

        assertTrue(
            newDistance <= sampledMinimum + 1e-4,
            "Expected solver distance $newDistance to match sampled minimum $sampledMinimum",
        )
    }

    @Test
    fun `invalid geometry inputs are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Line(Vec3.ZERO, Vec3.ZERO)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ray(Vec3.ZERO, Vec3.ZERO)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LineSegment(Vec3.ZERO, Vec3.ZERO)
        }
    }

    @Test
    fun `pointAtOrNull rejects non finite parameters`() {
        val line = Line(Vec3.ZERO, Vec3.X_AXIS)
        val ray = Ray(Vec3.ZERO, Vec3.X_AXIS)
        val segment = LineSegment(Vec3.ZERO, Vec3.X_AXIS)

        assertNull(line.pointAtOrNull(Double.NaN))
        assertNull(line.pointAtOrNull(Double.POSITIVE_INFINITY))
        assertNull(ray.pointAtOrNull(Double.NaN))
        assertNull(ray.pointAtOrNull(Double.POSITIVE_INFINITY))
        assertNull(segment.pointAtOrNull(Double.NaN))
        assertNull(segment.pointAtOrNull(Double.POSITIVE_INFINITY))
    }

    private fun assertPointOnGeometry(geometry: LinearGeometry3, point: Vec3) {
        val relative = point.subtract(geometry.anchor)
        val cross = relative.cross(geometry.direction)
        assertTrue(cross.lengthSqr() <= eps, "Point $point is not on geometry $geometry")
    }

    private fun assertPointInOrOnBox(point: Vec3, box: AABB, tolerance: Double = 1e-9) {
        assertTrue(point.x + tolerance >= box.minX && point.x - tolerance <= box.maxX, "x out of box: ${point.x}")
        assertTrue(point.y + tolerance >= box.minY && point.y - tolerance <= box.maxY, "y out of box: ${point.y}")
        assertTrue(point.z + tolerance >= box.minZ && point.z - tolerance <= box.maxZ, "z out of box: ${point.z}")
    }

    private fun assertNearSampledMinimum(
        line: Line,
        box: AABB,
        nearestDistance: Double,
        parameterRange: ClosedFloatingPointRange<Double>,
        step: Double
    ) {
        var sampledMin = Double.POSITIVE_INFINITY

        (parameterRange step step).forEachDouble { phi ->
            val sampledPoint = line.pointAtOrNull(phi)!!
            val sampledDistance = box.distanceToSqr(sampledPoint)
            if (sampledDistance < sampledMin) {
                sampledMin = sampledDistance
            }
        }

        assertTrue(
            nearestDistance <= sampledMin + 1e-6,
            "Expected nearest distance $nearestDistance to be close to sampled minimum $sampledMin"
        )
    }

    private fun sampleSegmentDistanceSqr(first: LineSegment, second: LineSegment, step: Double): Double {
        var sampledMin = Double.POSITIVE_INFINITY

        (0.0..1.0 step step).forEachDouble { firstPhi ->
            val pointOnFirst = first.pointAtOrNull(firstPhi)!!
            (0.0..1.0 step step).forEachDouble { secondPhi ->
                val pointOnSecond = second.pointAtOrNull(secondPhi)!!
                sampledMin = minOf(sampledMin, pointOnFirst.distanceToSqr(pointOnSecond))
            }
        }

        return sampledMin
    }
}
