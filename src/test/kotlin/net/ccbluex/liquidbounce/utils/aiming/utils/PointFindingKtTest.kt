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

package net.ccbluex.liquidbounce.utils.aiming.utils

import net.ccbluex.liquidbounce.test.assertVec3Equals
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PointFindingKtTest {

    @Test
    fun `rotation matrices are inverses`() {
        val normal = Vec3(-1.0, 2.0, -3.0).normalize()
        val (toMatrix, backMatrix) = getRotationMatricesForVec(normal)

        val arbitrary = Vec3(0.25, -0.4, 0.75)
        val restored = arbitrary.toVector3f().mul(backMatrix).mul(toMatrix)

        assertVec3Equals(arbitrary, restored.toVec3d(), 1e-6)
    }

    @Test
    fun `projected points lie on box surface`() {
        val eye = Vec3(-2.0, 0.5, 0.5)
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val points = projectPointsOnBox(eye, box, maxPoints = 32)

        assertNotNull(points)
        assertFalse(points!!.isEmpty())
        points.forEach { point ->
            assertPointOnBoxSurface(point, box)
        }
    }

    @Test
    fun `projected points report false when eye is inside box`() {
        val eye = Vec3(0.5, 0.5, 0.5)
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        val collected = ArrayList<Vec3>()

        val success = projectPointsOnBox(eye, box, maxPoints = 16) { collected += it }

        assertFalse(success)
        assertTrue(collected.isEmpty())
        assertEquals(null, projectPointsOnBox(eye, box, maxPoints = 16))
    }

    private fun assertPointOnBoxSurface(point: Vec3, box: AABB, tolerance: Double = 1e-6) {
        assertTrue(point.x + tolerance >= box.minX && point.x - tolerance <= box.maxX, "x out of bounds: ${point.x}")
        assertTrue(point.y + tolerance >= box.minY && point.y - tolerance <= box.maxY, "y out of bounds: ${point.y}")
        assertTrue(point.z + tolerance >= box.minZ && point.z - tolerance <= box.maxZ, "z out of bounds: ${point.z}")

        val touchesFace = almostEquals(point.x, box.minX, tolerance) ||
            almostEquals(point.x, box.maxX, tolerance) ||
            almostEquals(point.y, box.minY, tolerance) ||
            almostEquals(point.y, box.maxY, tolerance) ||
            almostEquals(point.z, box.minZ, tolerance) ||
            almostEquals(point.z, box.maxZ, tolerance)

        assertTrue(touchesFace, "Expected point to lie on box surface, got $point")
    }

    private fun almostEquals(a: Double, b: Double, tolerance: Double): Boolean {
        return kotlin.math.abs(a - b) <= tolerance
    }
}
