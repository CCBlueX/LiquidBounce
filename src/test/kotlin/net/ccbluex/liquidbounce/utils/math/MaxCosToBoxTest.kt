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
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class MaxCosToBoxTest {

    private val box = AABB(1.0, 0.5, -1.0, 2.0, 1.5, 1.0)
    private val forward = Vec3(1.0, 0.0, 0.0)

    @Test
    fun `ray through the box is a full hit`() {
        assertEquals(1.0, maxCosToBox(Vec3(0.0, 1.0, 0.0), forward, box), 1e-12)
    }

    @Test
    fun `eye inside the box is a full hit`() {
        assertEquals(1.0, maxCosToBox(Vec3(1.5, 1.0, 0.0), Vec3(0.0, 1.0, 0.0), box), 1e-12)
    }

    @Test
    fun `offset box resolves to the closest point of a silhouette edge`() {
        // The box sits above the cross-hair, so the nearest direction is the interior point
        // (2, 0.5, 0) of the edge its far face shares with its bottom face.
        assertEquals(2.0 / sqrt(4.25), maxCosToBox(Vec3.ZERO, forward, box), 1e-12)
    }

    @Test
    fun `box behind the eye stays negative`() {
        // Corner (2, 0.5, -1), reached as an endpoint of the edge from (1, 0.5, -1), is nearest
        assertEquals(-18.0 / sqrt(325.25), maxCosToBox(Vec3(20.0, 1.0, 0.0), forward, box), 1e-12)
    }

}
