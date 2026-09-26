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
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AlignedFaceTest {

    private val eps = 1e-9

    /** An xy face spanning [0, 2] x [0, 3] in the z = 0 plane. */
    private val sampleFace = AlignedFace(Vec3.ZERO, Vec3(2.0, 3.0, 0.0))

    // --- basic geometry ---

    @Test
    fun `area matches single xy face area`() {
        val face = AlignedFace(Vec3.ZERO, Vec3(2.0, 3.0, 0.0))

        assertEquals(6.0, face.area, 1e-9)
    }

    @Test
    fun `area matches single yz face area`() {
        val face = AlignedFace(Vec3.X_AXIS, Vec3(1.0, 2.0, 4.0))

        assertEquals(8.0, face.area, 1e-9)
    }

    @Test
    fun `area matches single xz face area`() {
        val face = AlignedFace(Vec3(0.0, 1.0, 0.0), Vec3(4.0, 1.0, 2.0))

        assertEquals(8.0, face.area, 1e-9)
    }

    @Test
    fun `area of a box is half its surface area`() {
        val face = AlignedFace(Vec3.ZERO, Vec3(1.0, 2.0, 3.0))

        assertEquals(11.0, face.area, 1e-9)
    }

    @Test
    fun `degenerate edge has zero area`() {
        val face = AlignedFace(Vec3(1.0, 2.0, 3.0), Vec3(1.0, 2.0, 7.0))

        assertEquals(0.0, face.area, 1e-9)
    }

    @Test
    fun `degenerate point has zero area`() {
        val face = AlignedFace(Vec3(1.0, 2.0, 3.0), Vec3(1.0, 2.0, 3.0))

        assertEquals(0.0, face.area, 1e-9)
    }

    @Test
    fun `constructor normalizes corners component wise`() {
        val face = AlignedFace(Vec3(5.0, 1.0, 7.0), Vec3(1.0, 6.0, 2.0))

        assertVec3Equals(Vec3(1.0, 1.0, 2.0), face.from, eps)
        assertVec3Equals(Vec3(5.0, 6.0, 7.0), face.to, eps)
    }

    @Test
    fun `dimensions are component wise differences`() {
        val face = AlignedFace(Vec3(1.0, 2.0, 3.0), Vec3(4.0, 6.0, 3.0))

        assertVec3Equals(Vec3(3.0, 4.0, 0.0), face.dimensions, eps)
    }

    @Test
    fun `center is the midpoint`() {
        val face = AlignedFace(Vec3.ZERO, Vec3(2.0, 4.0, 6.0))

        assertVec3Equals(Vec3(1.0, 2.0, 3.0), face.center, eps)
    }

    // --- state queries ---

    @Test
    fun `requireNonEmpty returns the face for real faces`() {
        assertSame(sampleFace, sampleFace.requireNonEmpty())
    }

    @Test
    fun `requireNonEmpty returns null for degenerate faces`() {
        val degenerate = AlignedFace(Vec3(1.0, 2.0, 3.0), Vec3(1.0, 2.0, 7.0))

        assertNull(degenerate.requireNonEmpty())
    }

    // --- transformations ---

    @Test
    fun `truncateY raises the lower edge to the threshold`() {
        val truncated = sampleFace.truncateY(1.5)

        assertVec3Equals(Vec3(0.0, 1.5, 0.0), truncated.from, eps)
        assertVec3Equals(Vec3(2.0, 3.0, 0.0), truncated.to, eps)
    }

    @Test
    fun `truncateY leaves faces above the threshold unchanged`() {
        val above = AlignedFace(Vec3(0.0, 5.0, 0.0), Vec3(2.0, 7.0, 0.0))

        val truncated = above.truncateY(4.0)

        assertVec3Equals(above.from, truncated.from, eps)
        assertVec3Equals(above.to, truncated.to, eps)
    }

    @Test
    fun `truncateY below the face degenerates it`() {
        val truncated = sampleFace.truncateY(5.0)

        assertEquals(0.0, truncated.area, eps)
        assertNull(truncated.requireNonEmpty())
        assertVec3Equals(Vec3(0.0, 5.0, 0.0), truncated.from, eps)
        assertVec3Equals(Vec3(2.0, 5.0, 0.0), truncated.to, eps)
    }

    @Test
    fun `clamp keeps the face unchanged inside the box`() {
        val box = AABB(-1.0, -1.0, -1.0, 5.0, 5.0, 5.0)

        val clamped = sampleFace.clamp(box)

        assertVec3Equals(sampleFace.from, clamped.from, eps)
        assertVec3Equals(sampleFace.to, clamped.to, eps)
    }

    @Test
    fun `clamp pulls corners into the box`() {
        val clamped = sampleFace.clamp(AABB(1.0, 1.0, 1.0, 5.0, 5.0, 5.0))

        assertVec3Equals(Vec3(1.0, 1.0, 1.0), clamped.from, eps)
        assertVec3Equals(Vec3(2.0, 3.0, 1.0), clamped.to, eps)
    }

    @Test
    fun `clamp against a box the face does not reach degenerates`() {
        val clamped = sampleFace.clamp(AABB(5.0, 5.0, 5.0, 10.0, 10.0, 10.0))

        assertEquals(0.0, clamped.area, eps)
    }

    @Test
    fun `offset translates the face`() {
        val offset = sampleFace.offset(Vec3(1.0, 2.0, 3.0))

        assertVec3Equals(Vec3(1.0, 2.0, 3.0), offset.from, eps)
        assertVec3Equals(Vec3(3.0, 5.0, 3.0), offset.to, eps)
    }

    @Test
    fun `offset translates the face by block coordinates`() {
        val offset = sampleFace.offset(Vec3i(1, -2, 3))

        assertVec3Equals(Vec3(1.0, -2.0, 3.0), offset.from, eps)
        assertVec3Equals(Vec3(3.0, 1.0, 3.0), offset.to, eps)
    }

    @Test
    fun `randomPointOnFace stays inside the face and pins the constant axis`() {
        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY

        repeat(200) {
            val point = sampleFace.randomPointOnFace()

            assertEquals(0.0, point.z)
            assertTrue(point.x in 0.0..2.0, "x out of bounds: ${point.x}")
            assertTrue(point.y in 0.0..3.0, "y out of bounds: ${point.y}")
            minX = minOf(minX, point.x)
            maxX = maxOf(maxX, point.x)
        }

        assertTrue(maxX - minX > 1.0, "Expected samples to spread over the face")
    }

    @Test
    fun `randomPointOnFace on a degenerate point returns that point`() {
        val point = AlignedFace(Vec3(1.0, 2.0, 3.0), Vec3(1.0, 2.0, 3.0)).randomPointOnFace()

        assertVec3Equals(Vec3(1.0, 2.0, 3.0), point, eps)
    }

    @Test
    fun `samplePointOnFace spreads a over x and b over y on an xy face`() {
        val point = sampleFace.samplePointOnFace(0.25, 0.75)

        assertVec3Equals(Vec3(0.5, 2.25, 0.0), point, eps)
    }

    @Test
    fun `samplePointOnFace on an xz face uses a for x and b for z`() {
        val face = AlignedFace(Vec3(0.0, 1.0, 0.0), Vec3(4.0, 1.0, 2.0))

        assertVec3Equals(Vec3(1.0, 1.0, 1.5), face.samplePointOnFace(0.25, 0.75), eps)
    }

    @Test
    fun `samplePointOnFace on a yz face uses a for y and b for z`() {
        val face = AlignedFace(Vec3(3.0, 0.0, 0.0), Vec3(3.0, 2.0, 4.0))

        assertVec3Equals(Vec3(3.0, 0.5, 3.0), face.samplePointOnFace(0.25, 0.75), eps)
    }

    @Test
    fun `samplePointOnFace maps proportions zero and one to the face bounds`() {
        assertVec3Equals(sampleFace.from, sampleFace.samplePointOnFace(0.0, 0.0), eps)
        assertVec3Equals(sampleFace.to, sampleFace.samplePointOnFace(1.0, 1.0), eps)
    }

    @Test
    fun `samplePointOnFace pins constant axes to the from coordinate`() {
        val edge = AlignedFace(Vec3(0.0, 2.0, 3.0), Vec3(4.0, 2.0, 3.0))

        assertVec3Equals(Vec3(1.0, 2.0, 3.0), edge.samplePointOnFace(0.25, 0.75), eps)
    }

    // --- plane / line interactions ---

    @Test
    fun `toPlane contains all face corners`() {
        val plane = sampleFace.toPlane()

        for (corner in listOf(
            Vec3(0.0, 0.0, 0.0),
            Vec3(2.0, 0.0, 0.0),
            Vec3(0.0, 3.0, 0.0),
            Vec3(2.0, 3.0, 0.0),
        )) {
            assertPointOnPlane(corner, plane)
        }
    }

    @Test
    fun `toPlane of an x constant face is the x plane`() {
        val xFace = AlignedFace(Vec3(3.0, 0.0, 0.0), Vec3(3.0, 2.0, 4.0))
        val plane = xFace.toPlane()

        for (corner in listOf(
            Vec3(3.0, 0.0, 0.0),
            Vec3(3.0, 2.0, 0.0),
            Vec3(3.0, 0.0, 4.0),
            Vec3(3.0, 2.0, 4.0),
        )) {
            assertPointOnPlane(corner, plane)
        }
    }

    @Test
    fun `coerceInFace cuts an in plane line down to the face crossing`() {
        val line = Line(Vec3(1.0, 1.0, 0.0), Vec3(1.0, 0.5, 0.0))

        val segment = sampleFace.coerceInFace(line)

        assertNotNull(segment)
        assertVec3Equals(Vec3(0.0, 0.5, 0.0), segment!!.start, eps)
        assertVec3Equals(Vec3(2.0, 1.5, 0.0), segment.end, eps)
    }

    @Test
    fun `coerceInFace endpoints lie on the line and the face boundary`() {
        val line = Line(Vec3(0.5, 0.2, 0.0), Vec3(1.0, 0.3, 0.0))

        val segment = sampleFace.coerceInFace(line)

        assertNotNull(segment)
        assertPointOnLine(line, segment!!.start)
        assertPointOnLine(line, segment.end)
        assertPointOnFaceBoundary(segment.start)
        assertPointOnFaceBoundary(segment.end)
    }

    @Test
    fun `coerceInFace projects a parallel offset line onto the nearest face segment`() {
        val line = Line(Vec3(0.0, 1.0, 1.0), Vec3.X_AXIS)

        val segment = sampleFace.coerceInFace(line)

        assertNotNull(segment)
        assertVec3Equals(Vec3(0.0, 1.0, 0.0), segment!!.start, eps)
        assertVec3Equals(Vec3(2.0, 1.0, 0.0), segment.end, eps)
    }

    @Test
    fun `coerceInFace returns null when the nearest points coincide`() {
        val line = Line(Vec3.ZERO, Vec3.Z_AXIS)

        assertNull(sampleFace.coerceInFace(line))
    }

    @Test
    fun `nearestPointTo returns the plane intersection when the line pierces the face`() {
        val line = Line(Vec3(1.0, 1.5, -5.0), Vec3.Z_AXIS)

        assertVec3Equals(Vec3(1.0, 1.5, 0.0), sampleFace.nearestPointTo(line), eps)
    }

    @Test
    fun `nearestPointTo clamps to the boundary when the intersection is outside`() {
        val line = Line(Vec3(5.0, 1.0, -5.0), Vec3.Z_AXIS)

        assertVec3Equals(Vec3(2.0, 1.0, 0.0), sampleFace.nearestPointTo(line), eps)
    }

    @Test
    fun `nearestPointTo handles lines parallel to the face`() {
        val line = Line(Vec3(0.0, 1.0, 5.0), Vec3.X_AXIS)

        assertVec3Equals(Vec3(0.0, 1.0, 0.0), sampleFace.nearestPointTo(line), eps)
    }

    @Test
    fun `nearestPointTo is no further from the line than any boundary point`() {
        val lines = listOf(
            Line(Vec3(1.0, 1.5, -5.0), Vec3.Z_AXIS),
            Line(Vec3(5.0, 1.0, -5.0), Vec3.Z_AXIS),
            Line(Vec3(0.0, 1.0, 5.0), Vec3.X_AXIS),
            Line(Vec3(-1.0, -1.0, 0.0), Vec3(1.0, 0.4, 0.0)),
        )

        for (line in lines) {
            val result = sampleFace.nearestPointTo(line)

            assertPointInFace(result)
            val sampledMinimum = sampleFaceBoundaryMinDistanceSqr(sampleFace, line, 0.005)
            assertTrue(
                line.distanceToSqr(result) <= sampledMinimum + 1e-6,
                "Expected $result to be a nearest point to $line, sampled minimum $sampledMinimum",
            )
        }
    }

    // --- helpers ---

    private fun assertPointOnLine(line: Line, point: Vec3, tolerance: Double = eps) {
        val relative = point.subtract(line.position)
        val cross = relative.cross(line.direction)
        assertTrue(cross.lengthSqr() <= tolerance, "Point $point is not on line $line")
    }

    private fun assertPointOnPlane(point: Vec3, plane: NormalizedPlane, tolerance: Double = eps) {
        val signedDistance = point.dot(plane.normalVec) - plane.pos.dot(plane.normalVec)
        assertTrue(
            abs(signedDistance) <= tolerance,
            "Point $point is not on plane ${plane.normalVec} @ ${plane.pos}, got $signedDistance",
        )
    }

    private fun assertPointInFace(point: Vec3, face: AlignedFace = sampleFace, tolerance: Double = eps) {
        assertTrue(point.x + tolerance >= face.from.x && point.x - tolerance <= face.to.x, "x out of face: ${point.x}")
        assertTrue(point.y + tolerance >= face.from.y && point.y - tolerance <= face.to.y, "y out of face: ${point.y}")
        assertTrue(point.z + tolerance >= face.from.z && point.z - tolerance <= face.to.z, "z out of face: ${point.z}")
    }

    private fun assertPointOnFaceBoundary(point: Vec3, face: AlignedFace = sampleFace, tolerance: Double = eps) {
        assertPointInFace(point, face, tolerance)

        val onBoundary = abs(point.x - face.from.x) <= tolerance ||
            abs(point.x - face.to.x) <= tolerance ||
            abs(point.y - face.from.y) <= tolerance ||
            abs(point.y - face.to.y) <= tolerance ||
            abs(point.z - face.from.z) <= tolerance ||
            abs(point.z - face.to.z) <= tolerance

        assertTrue(onBoundary, "Point $point is not on the boundary of face $face")
    }

    private fun sampleFaceBoundaryMinDistanceSqr(face: AlignedFace, line: Line, step: Double): Double {
        val from = face.from
        val to = face.to
        val edges = listOf(
            LineSegment(from, Vec3(to.x, from.y, from.z)),
            LineSegment(Vec3(to.x, from.y, from.z), to),
            LineSegment(from, Vec3(from.x, to.y, from.z)),
            LineSegment(Vec3(from.x, to.y, from.z), to),
        )

        var minimum = Double.POSITIVE_INFINITY
        for (edge in edges) {
            (0.0..1.0 step step).forEachDouble { parameter ->
                minimum = minOf(minimum, line.distanceToSqr(edge.pointAtOrNull(parameter)!!))
            }
        }
        return minimum
    }
}
