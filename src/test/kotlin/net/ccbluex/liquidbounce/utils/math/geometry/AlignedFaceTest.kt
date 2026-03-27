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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import net.minecraft.world.phys.Vec3

class AlignedFaceTest {

    @Test
    fun `area matches single xy face area`() {
        val face = AlignedFace(Vec3(0.0, 0.0, 0.0), Vec3(2.0, 3.0, 0.0))

        assertEquals(6.0, face.area, 1e-9)
    }

    @Test
    fun `area matches single yz face area`() {
        val face = AlignedFace(Vec3(1.0, 0.0, 0.0), Vec3(1.0, 2.0, 4.0))

        assertEquals(8.0, face.area, 1e-9)
    }

    @Test
    fun `degenerate edge has zero area`() {
        val face = AlignedFace(Vec3(1.0, 2.0, 3.0), Vec3(1.0, 2.0, 7.0))

        assertEquals(0.0, face.area, 1e-9)
    }
}
