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

import net.minecraft.world.phys.Vec3
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class PlaneSectionTest {

    @Test
    fun `fair point counts collapse a fully degenerate section to one point`() {
        val section = PlaneSection(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO)

        val (countDir1, countDir2) = section.getFairPointCounts(10)

        assertEquals(1, countDir1)
        assertEquals(1, countDir2)
    }

    @Test
    fun `fair point counts put every point on the only non-zero direction`() {
        val section = PlaneSection(Vec3.ZERO, Vec3.ZERO, Vec3(0.0, 0.0, 2.0))

        val (countDir1, countDir2) = section.getFairPointCounts(10)

        assertEquals(1, countDir1)
        assertEquals(10, countDir2)
    }

    @Test
    fun `fair point counts put every point on the non-zero first direction`() {
        val section = PlaneSection(Vec3.ZERO, Vec3(0.0, 2.0, 0.0), Vec3.ZERO)

        val (countDir1, countDir2) = section.getFairPointCounts(10)

        assertEquals(10, countDir1)
        assertEquals(1, countDir2)
    }

    @Test
    fun `fair point counts never exceed the requested amount`() {
        val sections = listOf(
            PlaneSection(Vec3.ZERO, Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0)),
            PlaneSection(Vec3.ZERO, Vec3(1.0, 0.0, 0.0), Vec3(0.0, 0.5, 0.0)),
            // 200:1 sides, far beyond the point budget itself.
            PlaneSection(Vec3.ZERO, Vec3(1.0, 0.0, 0.0), Vec3(0.0, 0.005, 0.0)),
            // The same extreme ratio the other way around.
            PlaneSection(Vec3.ZERO, Vec3(0.005, 0.0, 0.0), Vec3(0.0, 1.0, 0.0)),
        )

        for (section in sections) {
            for (nPoints in intArrayOf(1, 9, 25, 100, 128, 256, 361)) {
                val (countDir1, countDir2) = section.getFairPointCounts(nPoints)

                assertTrue(
                    countDir1 * countDir2 <= nPoints,
                    "nPoints=$nPoints produced ${countDir1 * countDir2} points",
                )
            }
        }
    }

    @Test
    fun `fair point counts follow the side length ratio`() {
        // A 1 x 0.5 section has 2:1 sides and therefore 2:1 counts.
        val section = PlaneSection(Vec3.ZERO, Vec3(1.0, 0.0, 0.0), Vec3(0.0, 0.5, 0.0))

        val (countDir1, countDir2) = section.getFairPointCounts(100)

        assertEquals(14, countDir1)
        assertEquals(7, countDir2)
    }

    @Test
    fun `fair point counts are square on a square section`() {
        val section = PlaneSection(Vec3.ZERO, Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0))

        val (countDir1, countDir2) = section.getFairPointCounts(256)

        assertEquals(16, countDir1)
        assertEquals(16, countDir2)
    }
}
