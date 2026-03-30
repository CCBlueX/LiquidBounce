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

import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaneSectionTest {

    @Test
    fun `fair step side handles fully degenerate section`() {
        val section = PlaneSection(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO)

        val (stepDir2, stepDir1) = section.getFairStepSide(10)

        assertEquals(1.0, stepDir2)
        assertEquals(1.0, stepDir1)
    }

    @Test
    fun `fair step side handles zero first direction without using aspect ratio`() {
        val section = PlaneSection(Vec3.ZERO, Vec3.ZERO, Vec3(0.0, 0.0, 2.0))

        val (stepDir2, stepDir1) = section.getFairStepSide(10)

        assertEquals(1.0, stepDir2)
        assertEquals(0.2, stepDir1)
    }

    @Test
    fun `fair step side handles zero second direction without using aspect ratio`() {
        val section = PlaneSection(Vec3.ZERO, Vec3(0.0, 2.0, 0.0), Vec3.ZERO)

        val (stepDir2, stepDir1) = section.getFairStepSide(10)

        assertEquals(0.2, stepDir2)
        assertEquals(1.0, stepDir1)
    }
}
