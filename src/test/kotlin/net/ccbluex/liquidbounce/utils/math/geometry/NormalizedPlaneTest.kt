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

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

class NormalizedPlaneTest {

    @Test
    fun `yz aligned planes still intersect in a line`() {
        val yPlane = NormalizedPlane(Vec3(0.0, 2.0, 0.0), Vec3(0.0, 1.0, 0.0))
        val zPlane = NormalizedPlane(Vec3(0.0, 0.0, 3.0), Vec3(0.0, 0.0, 1.0))

        val intersection = yPlane.intersection(zPlane)

        assertNotNull(intersection)
        assertLineLiesOnPlane(intersection, yPlane)
        assertLineLiesOnPlane(intersection, zPlane)
    }

    @Test
    fun `plane intersection anchor lies on both planes in xz branch`() {
        val yPlane = NormalizedPlane(Vec3(0.0, 2.0, 0.0), Vec3(0.0, 1.0, 0.0))
        val tiltedPlane = NormalizedPlane(Vec3(3.0, 2.0, 0.0), Vec3(1.0, 2.0, 0.0))

        val intersection = yPlane.intersection(tiltedPlane)

        assertNotNull(intersection)
        assertLineLiesOnPlane(intersection, yPlane)
        assertLineLiesOnPlane(intersection, tiltedPlane)
    }

    @Test
    fun `parallel planes do not intersect in a line`() {
        val first = NormalizedPlane(Vec3(0.0, 2.0, 0.0), Vec3(0.0, 1.0, 0.0))
        val second = NormalizedPlane(Vec3(0.0, 4.0, 0.0), Vec3(0.0, 1.0, 0.0))

        assertNull(first.intersection(second))
    }

    private fun assertLineLiesOnPlane(line: Line?, plane: NormalizedPlane, tolerance: Double = 1e-9) {
        requireNotNull(line)

        for (parameter in doubleArrayOf(-1.0, 0.0, 1.0)) {
            val point = line.pointAt(parameter)
            val signedDistance = point.dot(plane.normalVec) - plane.pos.dot(plane.normalVec)
            org.junit.jupiter.api.Assertions.assertTrue(
                abs(signedDistance) <= tolerance,
                "Expected point $point on plane ${plane.normalVec} @ ${plane.pos}, got signed distance $signedDistance",
            )
        }
    }
}
