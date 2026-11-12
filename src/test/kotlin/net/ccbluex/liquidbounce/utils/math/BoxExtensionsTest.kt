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

import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class BoxExtensionsTest {

    @Test
    fun `distanceToSqr xyz overload matches vanilla vec3 overload`() {
        val box = AABB(-1.5, 0.25, 2.0, 3.5, 4.0, 5.75)
        val points = listOf(
            Vec3(0.0, 1.0, 3.0),
            Vec3(-4.0, 1.0, 3.0),
            Vec3(5.0, 1.0, 3.0),
            Vec3(0.0, -2.0, 3.0),
            Vec3(0.0, 6.0, 3.0),
            Vec3(0.0, 1.0, -1.0),
            Vec3(0.0, 1.0, 8.0),
            Vec3(-4.0, -2.0, 3.0),
            Vec3(5.0, 6.0, 8.0),
            Vec3(-4.0, 6.0, -1.0),
        )

        for (point in points) {
            assertEquals(
                box.distanceToSqr(point),
                box.distanceToSqr(point.x, point.y, point.z),
                0.0,
                "distance mismatch for $point",
            )
        }
    }

    @Test
    fun `isHitByLine returns true when infinite line crosses box`() {
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        assertTrue(box.isHitByLine(Vec3(-2.0, 0.5, 0.5), Vec3(2.0, 0.5, 0.5)))
    }

    @Test
    fun `isHitByLine keeps infinite line semantics when target point is in front of start`() {
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        assertTrue(box.isHitByLine(Vec3(2.0, 0.5, 0.5), Vec3(3.0, 0.5, 0.5)))
    }

    @Test
    fun `isHitByLine degenerates to point containment when both points match`() {
        val box = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        assertTrue(box.isHitByLine(Vec3(0.5, 0.5, 0.5), Vec3(0.5, 0.5, 0.5)))
        assertFalse(box.isHitByLine(Vec3(1.5, 0.5, 0.5), Vec3(1.5, 0.5, 0.5)))
    }

    @Test
    fun `volume measures the box and overlapVolume ignores the gap between disjoint boxes`() {
        val box = AABB(0.0, 0.0, 0.0, 0.6, 1.8, 0.6)
        val disjoint = AABB(2.0, 0.0, 0.0, 3.0, 1.0, 1.0)

        assertEquals(0.6 * 1.8 * 0.6, box.volume, 1e-9)
        assertEquals(0.6 * 1.0 * 0.6, box.overlapVolume(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)), 1e-9)
        assertEquals(0.0, box.overlapVolume(disjoint), 1e-9)
        assertTrue(box.intersect(disjoint).volume > 0.0)
    }
}
