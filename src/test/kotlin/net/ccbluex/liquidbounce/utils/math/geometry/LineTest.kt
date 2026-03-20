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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LineTest {

    private val eps = 1e-9

    @Test
    fun `nearest point is on line and inside box when line intersects box`() {
        val line = Line(position = Vec3(-2.0, 0.5, 0.5), direction = Vec3(1.0, 0.0, 0.0))
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val nearest = line.getNearestPointTo(box)

        assertPointOnLine(line, nearest)
        assertTrue(box.contains(nearest), "Expected nearest point to be inside box, got $nearest")
        assertEquals(0.0, squaredDistanceToBox(nearest, box), 1e-9)
    }

    @Test
    fun `nearest point for non intersecting line minimizes distance to box`() {
        val line = Line(position = Vec3(2.0, 2.0, 2.0), direction = Vec3(1.0, 1.0, 0.0))
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val nearest = line.getNearestPointTo(box)

        assertPointOnLine(line, nearest)
        val nearestDistance = squaredDistanceToBox(nearest, box)
        assertEquals(1.0, nearestDistance, 1e-8)
        assertNearSampledMinimum(line, box, nearestDistance, -5.0..5.0, 0.01)
    }

    @Test
    fun `line segment nearest point respects phi range and can clamp to endpoint`() {
        val segment = LineSegment(
            position = Vec3(2.0, 2.0, 2.0),
            direction = Vec3(1.0, 0.0, 0.0),
            phiRange = 0.0..1.0
        )
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val nearest = segment.getNearestPointTo(box)
        val expectedEndpoint = segment.getPosition(0.0)

        assertPointOnLine(segment, nearest)
        assertVec3Equals(expectedEndpoint, nearest, 1e-8)
    }

    @Test
    fun `line starting inside box has zero distance`() {
        val line = Line(position = Vec3(0.25, 0.25, 0.25), direction = Vec3(1.0, 2.0, 3.0))
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val nearest = line.getNearestPointTo(box)

        assertPointOnLine(line, nearest)
        assertTrue(box.contains(nearest), "Expected nearest point to stay in box, got $nearest")
        assertEquals(0.0, line.distanceToSqr(box), 1e-9)
    }

    @Test
    fun `axis parallel line outside one slab has constant distance`() {
        val line = Line(position = Vec3(2.0, -5.0, 0.5), direction = Vec3(0.0, 1.0, 0.0))
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val nearest = line.getNearestPointTo(box)
        val nearestDistance = squaredDistanceToBox(nearest, box)

        assertPointOnLine(line, nearest)
        assertEquals(1.0, nearestDistance, 1e-8)
        assertEquals(nearestDistance, line.distanceToSqr(box), 1e-8)
    }

    @Test
    fun `line touching box boundary has zero distance`() {
        val line = Line(position = Vec3(-2.0, 1.0, 0.5), direction = Vec3(1.0, 0.0, 0.0))
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val nearest = line.getNearestPointTo(box)

        assertPointOnLine(line, nearest)
        assertTrue(nearest.y in 0.0..1.0 && nearest.z in 0.0..1.0, "Unexpected boundary point: $nearest")
        assertEquals(0.0, squaredDistanceToBox(nearest, box), 1e-9)
    }

    @Test
    fun `distanceToSqr box is consistent with nearest point result`() {
        val line = Line(position = Vec3(2.0, 2.0, 2.0), direction = Vec3(0.0, 0.0, 1.0))
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val pointOnLine = line.getNearestPointTo(box)
        val pointOnBox = box.getNearestPoint(pointOnLine)

        assertPointOnLine(line, pointOnLine)
        assertPointInOrOnBox(pointOnBox, box)
        assertEquals(pointOnLine.distanceToSqr(pointOnBox), line.distanceToSqr(box), 1e-8)
    }

    private fun assertPointOnLine(line: Line, point: Vec3) {
        val relative = point.subtract(line.position)
        val cross = relative.cross(line.direction)
        assertTrue(cross.lengthSqr() <= eps, "Point $point is not on line $line")
    }

    private fun squaredDistanceToBox(point: Vec3, box: AABB): Double {
        val nearestOnBox = box.getNearestPoint(point)
        return point.distanceToSqr(nearestOnBox)
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
        phiRange: ClosedFloatingPointRange<Double>,
        step: Double
    ) {
        var sampledMin = Double.POSITIVE_INFINITY

        (phiRange step step).forEachDouble { phi ->
            val sampledPoint = line.getPosition(phi)
            val sampledDistance = squaredDistanceToBox(sampledPoint, box)
            if (sampledDistance < sampledMin) {
                sampledMin = sampledDistance
            }
        }

        assertTrue(
            nearestDistance <= sampledMin + 1e-6,
            "Expected nearest distance $nearestDistance to be close to sampled minimum $sampledMin"
        )
    }
}
