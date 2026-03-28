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

import net.ccbluex.liquidbounce.test.assertVec3Equals
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BoxExtensionsTest {

    @Test
    fun `samplePointOnSide keeps local aabb offset for east face`() {
        val box = AABB(0.25, 0.1, 0.4, 0.75, 0.6, 0.9)

        val point = box.samplePointOnSide(Direction.EAST, 0.5, 0.25)

        assertVec3Equals(Vec3(0.75, 0.35, 0.525), point, 1e-9)
    }

    @Test
    fun `samplePointOnSide keeps local aabb offset for up face`() {
        val box = AABB(0.2, 0.3, 0.4, 0.8, 0.5, 0.9)

        val point = box.samplePointOnSide(Direction.UP, 0.25, 0.75)

        assertVec3Equals(Vec3(0.35, 0.5, 0.775), point, 1e-9)
    }

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
}
