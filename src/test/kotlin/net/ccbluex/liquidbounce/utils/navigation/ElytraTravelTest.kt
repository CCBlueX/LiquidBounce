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

package net.ccbluex.liquidbounce.utils.navigation

import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.navigation.TestWorld.Companion.AIR
import net.ccbluex.liquidbounce.utils.navigation.TestWorld.Companion.feet
import net.ccbluex.liquidbounce.utils.navigation.glide.ColumnHeights
import net.ccbluex.liquidbounce.utils.navigation.glide.Cruise
import net.ccbluex.liquidbounce.utils.navigation.glide.RoofBounce
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ElytraTravelTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }

        private const val ROCKET_TICKS = 20
    }

    @Test
    fun `a far cruise needs more rockets than a near one`() {
        val near = Cruise.rocketsNeeded(300.0, 10.0, ROCKET_TICKS)
        val far = Cruise.rocketsNeeded(1200.0, 10.0, ROCKET_TICKS)
        val high = Cruise.rocketsNeeded(300.0, 80.0, ROCKET_TICKS)

        assertTrue(far > near * 3, "1200 blocks took $far rockets, 300 took $near")
        assertTrue(high > near, "climbing 80 blocks took no extra rockets")
        assertEquals(Int.MAX_VALUE, Cruise.rocketsNeeded(300.0, 0.0, 0))
    }

    @Test
    fun `the cruise climbs over what is on the line and stays higher over unloaded columns`() {
        val start = Vec3(0.5, 100.0, 0.5)
        val goal = Vec3(300.5, 110.0, 0.5)
        val pillar = ColumnHeights { x, _ -> if (x in 150..152) 160 else 0 }
        val unloaded = ColumnHeights { x, _ -> if (x > 50) null else 0 }

        assertEquals(160.0 + Cruise.CLEARANCE, Cruise.cruiseAltitude(start, goal, pillar))
        assertEquals(
            110.0 + Cruise.CLEARANCE + Cruise.UNKNOWN_CLEARANCE,
            Cruise.cruiseAltitude(start, goal, unloaded),
        )
    }

    @Test
    fun `a flat open roof is a bounce line, a wall, a step or a hole ahead ends it`() {
        val line = RoofBounce.bounceLine(TestWorld.flat().terrain(), feet(0, 1, 0), feet(35, 1, 0), 24.0)
        assertNotNull(line)
        assertEquals(24.0, line.x - 0.5, 1.0E-6)

        val wall = TestWorld.flat().fill(10, 1, -2, 10, 3, 2)
        val step = TestWorld.flat().fill(10, 1, -2, 12, 1, 2)
        val hole = TestWorld.flat().set(12, 0, 0, AIR)
        val low = TestWorld.flat().fill(10, 3, -2, 12, 3, 2)
        for (world in listOf(wall, step, hole, low)) {
            assertNull(RoofBounce.bounceLine(world.terrain(), feet(0, 1, 0), feet(35, 1, 0), 24.0))
        }
    }

    @Test
    fun `unloaded ground ahead still bounces once enough is known`() {
        val world = TestWorld.flat { x, _ -> x < 16 }
        val line = assertNotNull(RoofBounce.bounceLine(world.terrain(), feet(0, 1, 0), feet(200, 1, 0), 24.0))
        assertTrue(line.x in 8.0..16.0, "the line ended at ${line.x}")

        val close = TestWorld.flat { x, _ -> x < 4 }
        assertNull(RoofBounce.bounceLine(close.terrain(), feet(0, 1, 0), feet(200, 1, 0), 24.0))
    }

}
